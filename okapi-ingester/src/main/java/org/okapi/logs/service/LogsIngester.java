package org.okapi.logs.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.io.ForwardedLogIngestRecord;
import org.okapi.logs.io.LogIngestRecord;
import org.okapi.logs.io.LogRecordTranslator;
import org.okapi.otel.ResourceAttributesReader;
import org.okapi.sharding.ShardAssigner;
import org.okapi.wal.frame.WalEntry;
import org.okapi.wal.io.IllegalWalEntryException;

@Slf4j
@RequiredArgsConstructor
public class LogsIngester {
  private final LogsCfg logsCfg;
  private final WalResourcesPerStream<Integer> walResourcesPerStream;
  private final ShardAssigner<String> shardAssigner;
  private final Gson gson = new Gson();

  public void ingest(ExportLogsServiceRequest req) throws IllegalWalEntryException, IOException {
    for (var resourceLogs : req.getResourceLogsList()) {
      var maybeSvc = ResourceAttributesReader.getSvc(resourceLogs.getResource());
      if (maybeSvc.isEmpty()) continue;
      var svc = maybeSvc.get();
      var groups = ArrayListMultimap.<Integer, LogRecord>create();
      for (var scopeLogs : resourceLogs.getScopeLogsList()) {
        groupByAndMerge(svc, groups, scopeLogs.getLogRecordsList());
      }
      consumeGroup(svc, groups);
    }
  }

  private void groupByAndMerge(
      String svc, Multimap<Integer, LogRecord> groups, List<LogRecord> logsRecords) {
    for (var record : logsRecords) {
      var tsNanos = record.getTimeUnixNano();
      var block = tsNanos / 1_000_000 / logsCfg.getIdxExpiryDuration();
      var shard = shardAssigner.getShardForStream(svc, block);
      groups.put(shard, record);
    }
  }

  protected void consumeGroup(String svc, Multimap<Integer, LogRecord> groups)
      throws IllegalWalEntryException, IOException {
    for (var shard : groups.keySet()) {
      var batch = groups.get(shard);
      var walEntries =
          batch.stream()
              .map(
                  record ->
                      this.toWalEntry(shard, LogRecordTranslator.toLogIngestRecord(svc, record)))
              .toList();
      var writer = walResourcesPerStream.getWalWriter(shard);
      writer.appendBatch(walEntries);
    }
  }

  public WalEntry toWalEntry(int shard, LogIngestRecord logRecord) {
    var lsnSupplier = walResourcesPerStream.getLsnSupplier(shard);
    var lsn = lsnSupplier.getLsn();
    var bytes = gson.toJson(logRecord).getBytes();
    return new WalEntry(lsn, bytes);
  }

  public void ingestForwarded(ForwardedLogIngestRecord records)
      throws IllegalWalEntryException, IOException {
    var walBatch =
        records.getRecords().stream().map(r -> toWalEntry(records.getShard(), r)).toList();
    walResourcesPerStream.getWalWriter(records.getShard()).appendBatch(walBatch);
  }
}
