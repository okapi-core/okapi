package org.okapi.traces.storage.dao;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import org.okapi.traces.cas.dao.TracesDao;
import org.okapi.traces.cas.dto.*;
import org.okapi.traces.model.OkapiSpan;
import org.okapi.traces.storage.TraceRepository;
import org.okapi.traces.util.TimeBuckets;

public class DaoTraceRepository implements TraceRepository {

  private final TracesDao dao;
  private final ObjectMapper mapper = new ObjectMapper();

  public DaoTraceRepository(TracesDao dao) {
    this.dao = dao;
  }

  @Override
  public void saveBatch(List<OkapiSpan> okapiSpans) {
    if (okapiSpans == null || okapiSpans.isEmpty()) return;
    for (OkapiSpan s : okapiSpans) {
      long startMs = s.getStartTime().toEpochMilli();
      long endMs = s.getEndTime() != null ? s.getEndTime().toEpochMilli() : startMs;
      long duration = s.getDurationMillis();
      long bucketSec = TimeBuckets.secondBucket(startMs);
      String status = normalizeStatus(s.getStatusCode());

      dao.saveSpan(
          CasSpan.builder()
              .tenantId(s.getTenantId())
              .traceId(s.getTraceId())
              .spanId(s.getSpanId())
              .parentSpanId(s.getParentSpanId())
              .name(s.getName())
              .startTimeMs(startMs)
              .endTimeMs(endMs)
              .durationMs(duration)
              .kind(s.getKind())
              .statusCode(status)
              .statusMessage(s.getStatusMessage())
              .attributesBlob(toBuffer(s.getAttributes()))
              .eventsBlob(toBuffer(s.getEvents()))
              .build());

      dao.saveSpanByTime(
          CasSpanByTime.builder()
              .tenantId(s.getTenantId())
              .bucketSecond(bucketSec)
              .statusCode(status)
              .startTimeMs(startMs)
              .traceId(s.getTraceId())
              .spanId(s.getSpanId())
              .durationMs(duration)
              .build());

      dao.saveSpanByDuration(
          CasSpanByDuration.builder()
              .tenantId(s.getTenantId())
              .bucketSecond(bucketSec)
              .durationMs(duration)
              .traceId(s.getTraceId())
              .spanId(s.getSpanId())
              .startTimeMs(startMs)
              .build());

      dao.saveTraceByTime(
          TraceByTime.builder()
              .tenantId(s.getTenantId())
              .bucketSecond(bucketSec)
              .traceId(s.getTraceId())
              .firstStartTimeMs(startMs)
              .lastEndTimeMs(endMs)
              .hasError("ERROR".equals(status))
              .build());

      dao.saveSpanById(
          CasSpanById.builder()
              .tenantId(s.getTenantId())
              .spanId(s.getSpanId())
              .traceId(s.getTraceId())
              .build());
    }
  }

  @Override
  public List<OkapiSpan> getSpansByTraceId(String traceId, String tenant) {
    PagingIterable<CasSpan> rows = dao.getSpansByTrace(tenant, traceId);
    List<OkapiSpan> out = new ArrayList<>();
    for (CasSpan row : rows) {
      OkapiSpan s = new OkapiSpan();
      s.setTenantId(tenant);
      s.setTraceId(traceId);
      s.setSpanId(row.getSpanId());
      s.setParentSpanId(row.getParentSpanId());
      s.setStartTime(Instant.ofEpochMilli(row.getStartTimeMs()));
      s.setEndTime(Instant.ofEpochMilli(row.getEndTimeMs()));
      s.setDurationMillis(row.getDurationMs());
      s.setName(row.getName());
      s.setKind(row.getKind());
      s.setStatusCode(row.getStatusCode());
      s.setStatusMessage(row.getStatusMessage());
      s.setAttributes(fromBufferMap(row.getAttributesBlob()));
      s.setEvents(fromBufferListMap(row.getEventsBlob()));
      out.add(s);
    }
    return out;
  }

  @Override
  public Optional<OkapiSpan> getSpanById(String spanId, String tenant) {
    CasSpanById row = dao.getSpanById(tenant, spanId);
    if (row == null) return Optional.empty();
    String traceId = row.getTraceId();
    return getSpansByTraceId(traceId, tenant).stream()
        .filter(s -> spanId.equals(s.getSpanId()))
        .findFirst();
  }

  @Override
  public List<OkapiSpan> listSpansByDuration(String tenant, long startMillis, long endMillis, int limit) {
    List<OkapiSpan> result = new ArrayList<>();
    for (long sec = TimeBuckets.secondBucket(startMillis); sec <= TimeBuckets.secondBucket(endMillis); sec++) {
      for (CasSpanByDuration row : dao.getSpansByDuration(tenant, sec)) {
        long start = row.getStartTimeMs();
        if (start < startMillis || start > endMillis) continue;
        OkapiSpan s = new OkapiSpan();
        s.setTenantId(tenant);
        s.setTraceId(row.getTraceId());
        s.setSpanId(row.getSpanId());
        s.setDurationMillis(row.getDurationMs());
        s.setStartTime(Instant.ofEpochMilli(start));
        result.add(s);
      }
    }
    result.sort(Comparator.comparingLong(OkapiSpan::getDurationMillis).reversed());
    if (result.size() > limit) return result.subList(0, limit);
    return result;
  }

  @Override
  public Map<String, Object> listTracesByWindow(String tenant, long startMillis, long endMillis) {
    Set<String> traces = new HashSet<>();
    Set<String> errorTraces = new HashSet<>();
    for (long sec = TimeBuckets.secondBucket(startMillis); sec <= TimeBuckets.secondBucket(endMillis); sec++) {
      for (TraceByTime t : dao.getTracesByTime(tenant, sec)) {
        traces.add(t.getTraceId());
      }
      for (CasSpanByTime row : dao.getSpansByTimeAndStatus(tenant, sec, "ERROR")) {
        long start = row.getStartTimeMs();
        if (start < startMillis || start > endMillis) continue;
        errorTraces.add(row.getTraceId());
      }
    }
    Map<String, Object> out = new HashMap<>();
    out.put("traces", new ArrayList<>(traces));
    out.put("errorCount", errorTraces.size());
    return out;
  }

  @Override
  public List<OkapiSpan> listErrorSpans(String tenant, long startMillis, long endMillis, int limit) {
    List<OkapiSpan> result = new ArrayList<>();
    for (long sec = TimeBuckets.secondBucket(startMillis); sec <= TimeBuckets.secondBucket(endMillis); sec++) {
      for (CasSpanByTime row : dao.getSpansByTimeAndStatus(tenant, sec, "ERROR")) {
        long start = row.getStartTimeMs();
        if (start < startMillis || start > endMillis) continue;
        OkapiSpan s = new OkapiSpan();
        s.setTenantId(tenant);
        s.setTraceId(row.getTraceId());
        s.setSpanId(row.getSpanId());
        s.setStartTime(Instant.ofEpochMilli(start));
        s.setDurationMillis(row.getDurationMs());
        result.add(s);
        if (result.size() >= limit) return result;
      }
    }
    return result;
  }

  @Override
  public Map<Long, Map<String, Long>> spanHistogramByMinute(String tenant, long startMillis, long endMillis) {
    Map<Long, Map<String, Long>> out = new TreeMap<>();
    for (long sec = TimeBuckets.secondBucket(startMillis); sec <= TimeBuckets.secondBucket(endMillis); sec++) {
      // Fetch all rows for the second and roll them up
      for (CasSpanByTime row : dao.getSpansByTime(tenant, sec)) {
        long start = row.getStartTimeMs();
        if (start < startMillis || start > endMillis) continue;
        long minute = start / 60000L;
        String status = normalizeStatus(row.getStatusCode());
        Map<String, Long> m = out.computeIfAbsent(minute, k -> new HashMap<>());
        m.put(status.equals("ERROR") ? "error" : "ok", m.getOrDefault(status.equals("ERROR") ? "error" : "ok", 0L) + 1);
      }
    }
    // fill empty minutes
    for (long m = startMillis / 60000L; m <= endMillis / 60000L; m++) {
      out.putIfAbsent(m, Map.of("ok", 0L, "error", 0L));
      out.computeIfPresent(
          m,
          (k, v) -> Map.of("ok", v.getOrDefault("ok", 0L), "error", v.getOrDefault("error", 0L)));
    }
    return out;
  }

  private ByteBuffer toBuffer(Object obj) {
    if (obj == null) return null;
    try {
      byte[] bytes = mapper.writeValueAsBytes(obj);
      return ByteBuffer.wrap(bytes);
    } catch (Exception e) {
      return null;
    }
  }

  private Map<String, String> fromBufferMap(ByteBuffer buf) {
    if (buf == null) return Collections.emptyMap();
    try {
      ByteBuffer dup = buf.duplicate();
      byte[] arr = new byte[dup.remaining()];
      dup.get(arr);
      return mapper.readValue(arr, new TypeReference<Map<String, String>>() {});
    } catch (Exception e) {
      return Collections.emptyMap();
    }
  }

  private List<Map<String, String>> fromBufferListMap(ByteBuffer buf) {
    if (buf == null) return Collections.emptyList();
    try {
      ByteBuffer dup = buf.duplicate();
      byte[] arr = new byte[dup.remaining()];
      dup.get(arr);
      return mapper.readValue(arr, new TypeReference<List<Map<String, String>>>() {});
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  private static String normalizeStatus(String status) {
    if (status == null) return "UNSET";
    String s = status.toUpperCase(Locale.ROOT);
    if (s.contains("ERROR") || s.equals("2")) return "ERROR";
    if (s.contains("OK") || s.equals("1")) return "OK";
    return "UNSET";
  }
}

