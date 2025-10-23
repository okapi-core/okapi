package org.okapi.logs.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.okapi.logs.StaticConfiguration;
import org.okapi.logs.config.LogsCfgImpl;
import org.okapi.logs.forwarding.LogForwarder;
import org.okapi.logs.mappers.OtelToLogMapper;
import org.okapi.logs.runtime.LogPageBufferPool;
import org.okapi.logs.select.BlockMemberSelector;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.ping.Member;
import org.okapi.swim.ping.MemberList;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OtelLogsController {
  private final LogPageBufferPool bufferPool;
  private final MeterRegistry meterRegistry;
  private final MemberList memberList;
  private final WhoAmI whoAmI;
  private final LogForwarder forwarder;
  private final BlockMemberSelector selector;
  private final LogsCfgImpl cfg;

  public OtelLogsController(
      LogPageBufferPool bufferPool,
      MeterRegistry meterRegistry,
      MemberList memberList,
      WhoAmI whoAmI,
      LogForwarder forwarder,
      BlockMemberSelector selector,
      LogsCfgImpl cfg) {
    this.bufferPool = bufferPool;
    this.meterRegistry = meterRegistry;
    this.memberList = memberList;
    this.whoAmI = whoAmI;
    this.forwarder = forwarder;
    this.selector = selector;
    this.cfg = cfg;
  }

  @PostMapping(path = "/v1/logs", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<String> ingestProtobuf(
      @RequestHeader("X-Okapi-Tenant-Id") String tenantId,
      @RequestHeader("X-Okapi-Log-Stream") String logStream,
      @RequestBody byte[] body)
      throws IOException {
    var req = ExportLogsServiceRequest.parseFrom(body);
    var myblock = StaticConfiguration.rkHash(whoAmI.getNodeId());
    var count = 0;
    var outOfBlock = new HashMap<Integer, List<LogRecord>>();
    for (var resourceLog : req.getResourceLogsList()) {
      for (var scopeLogs : resourceLog.getScopeLogsList()) {
        for (LogRecord logRecord : scopeLogs.getLogRecordsList()) {
          long tsMs = logRecord.getTimeUnixNano() / 1_000_000L;
          var hr = tsMs / cfg.getIdxExpiryDuration();
          var blockIdx = StaticConfiguration.hashLogStream(tenantId, logStream, hr);
          if (blockIdx == myblock) {
            int level = OtelToLogMapper.mapLevel(logRecord.getSeverityNumber().getNumber());
            String traceId = OtelToLogMapper.traceIdToHex(logRecord.getTraceId());
            String text = OtelToLogMapper.anyValueToString(logRecord.getBody());
            bufferPool.consume(tenantId, logStream, tsMs, traceId, level, text);
            count++;
          } else {
            outOfBlock.putIfAbsent(blockIdx, new ArrayList<>());
            outOfBlock.get(blockIdx).add(logRecord);
          }
        }
      }

      for (var block : outOfBlock.keySet()) {
        var records = outOfBlock.get(block);
        long firstTsMs = records.get(0).getTimeUnixNano() / 1_000_000L;
        long hourStart = (firstTsMs / cfg.getIdxExpiryDuration()) * cfg.getIdxExpiryDuration();
        Member member = selector.select(tenantId, logStream, hourStart, block, memberList);
        if (member.getNodeId().equals(whoAmI.getNodeId())) {
          for (var record : records) {
            bufferPool.consume(tenantId, logStream, record);
          }
        } else {
          forwarder.forward(tenantId, logStream, member, records);
        }
      }
    }
    meterRegistry.counter("okapi.logs.ingest_records_total").increment(count);
    return ResponseEntity.ok("OK");
  }

  @PostMapping(path = "/v1/logs/bulk", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<String> bulkIngest(
      @RequestHeader("X-Okapi-Tenant-Id") String tenantId,
      @RequestHeader("X-Okapi-Log-Stream") String logStream,
      @RequestBody byte[] body)
      throws InvalidProtocolBufferException {
    var req = ScopeLogs.parseFrom(body);
    for (LogRecord logRecord : req.getLogRecordsList()) {
      bufferPool.consume(tenantId, logStream, logRecord);
    }
    return ResponseEntity.ok("OK");
  }
}
