package org.okapi.traces.storage;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.Range;
import com.apple.foundationdb.tuple.Tuple;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.okapi.traces.model.Span;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TraceRepository {

  private final ObjectMapper objectMapper;
  private Database db;

  public static final int API_VERSION = 730; // FoundationDB 7.3
  public static final int DURATION_SHARDS = 16;

  @PostConstruct
  public void init() {
    FDB fdb = FDB.selectAPIVersion(API_VERSION);
    db = fdb.open();
  }

  private static long hourBucketEpochMillis(long epochMillis) {
    ZonedDateTime zdt = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC);
    return zdt.withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli();
  }

  private static int shardForSpanId(String spanId) {
    return (spanId == null) ? 0 : (Math.abs(spanId.hashCode()) % DURATION_SHARDS);
  }

  public void saveBatch(List<Span> spans) {
    if (spans == null || spans.isEmpty()) return;
    db.run(
        tr -> {
          for (Span s : spans) {
            try {
              // Primary
              byte[] pkey =
                  Tuple.from("tr", s.getTenantId(), s.getAppId(), s.getTraceId(), s.getSpanId())
                      .pack();
              byte[] pval = objectMapper.writeValueAsBytes(s);
              tr.set(pkey, pval);

              // SpanId -> TraceId
              byte[] sidKey =
                  Tuple.from("sid", s.getTenantId(), s.getAppId(), s.getSpanId()).pack();
              tr.set(sidKey, Tuple.from(s.getTraceId()).pack());

              // Duration index
              long bucket = hourBucketEpochMillis(s.getStartTime().toEpochMilli());
              int shard = shardForSpanId(s.getSpanId());
              long negDur = -s.getDurationMillis();
              byte[] dkey =
                  Tuple.from(
                          "dur",
                          s.getTenantId(),
                          s.getAppId(),
                          bucket,
                          shard,
                          negDur,
                          s.getTraceId(),
                          s.getSpanId())
                      .pack();
              tr.set(dkey, new byte[0]);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
          return null;
        });
  }

  public List<Span> getSpansByTraceId(String traceId, String tenant, String app) {
    byte[] prefix = Tuple.from("tr", tenant, app, traceId).pack();
    Range r = Range.startsWith(prefix);
    return db.read(
        tr -> {
          List<KeyValue> kvs = tr.getRange(r).asList().join();
          List<Span> out = new ArrayList<>(kvs.size());
          for (KeyValue kv : kvs) {
            try {
              out.add(objectMapper.readValue(kv.getValue(), Span.class));
            } catch (Exception e) {
              // ignore corrupt records
            }
          }
          return out;
        });
  }

  public Optional<Span> getSpanById(String spanId, String tenant, String app) {
    return db.read(
        tr -> {
          byte[] sidKey = Tuple.from("sid", tenant, app, spanId).pack();
          byte[] traceTuple = tr.get(sidKey).join();
          if (traceTuple == null) return Optional.empty();
          Tuple t = Tuple.fromBytes(traceTuple);
          String traceId = t.getString(0);
          byte[] pkey = Tuple.from("tr", tenant, app, traceId, spanId).pack();
          byte[] pval = tr.get(pkey).join();
          if (pval == null) return Optional.empty();
          try {
            return Optional.of(objectMapper.readValue(pval, Span.class));
          } catch (Exception e) {
            return Optional.empty();
          }
        });
  }

  public List<Span> listSpansByDuration(
      String tenant, String app, long startEpochMillis, long endEpochMillis, int limit) {
    if (limit <= 0) limit = 100;
    long startHr = hourBucketEpochMillis(startEpochMillis);
    long endHr = hourBucketEpochMillis(endEpochMillis);
    List<Span> acc = new ArrayList<>();

      int finalLimit = limit;
      return db.read(
        tr -> {
          List<SpanCandidate> candidates = new ArrayList<>();
          for (long hr = startHr; hr <= endHr; hr += 3600_000L) {
            for (int shard = 0; shard < DURATION_SHARDS; shard++) {
              byte[] prefix = Tuple.from("dur", tenant, app, hr, shard).pack();
              Range range = Range.startsWith(prefix);
              List<KeyValue> kvs = tr.getRange(range).asList().join();
              for (KeyValue kv : kvs) {
                Tuple key = Tuple.fromBytes(kv.getKey());
                long negDur = ((Number) key.get(5)).longValue();
                String traceId = (String) key.get(6);
                String spanId = (String) key.get(7);
                long duration = -negDur;
                candidates.add(new SpanCandidate(duration, traceId, spanId));
              }
            }
          }

          // Deduplicate and pick top by duration
          candidates =
              candidates.stream()
                  .collect(
                      Collectors.toMap(
                          c -> c.traceId + ":" + c.spanId,
                          c -> c,
                          (a, b) -> a.duration >= b.duration ? a : b))
                  .values()
                  .stream()
                  .sorted(Comparator.comparingLong((SpanCandidate c) -> c.duration).reversed())
                  .limit(finalLimit)
                  .collect(Collectors.toList());

          List<Span> spans = new ArrayList<>(candidates.size());
          for (SpanCandidate c : candidates) {
            byte[] pkey = Tuple.from("tr", tenant, app, c.traceId, c.spanId).pack();
            byte[] pval = tr.get(pkey).join();
            if (pval == null) continue;
            try {
              Span s = objectMapper.readValue(pval, Span.class);
              // filter by actual time window
              long start = Objects.requireNonNull(s.getStartTime()).toEpochMilli();
              if (start >= startEpochMillis && start <= endEpochMillis) {
                spans.add(s);
              }
            } catch (Exception ignored) {
            }
          }
          return spans;
        });
  }

  private record SpanCandidate(long duration, String traceId, String spanId) {}
}
