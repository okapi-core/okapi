package org.okapi.logs.io;

import io.opentelemetry.proto.logs.v1.LogRecord;
import java.math.BigInteger;
import java.util.Random;
import org.apache.commons.codec.binary.Hex;
import org.okapi.primitives.BinaryLogRecordV1;

public class LogRecordTranslator {
  public static String getRandomDocId() {
    var random = new Random();
    var bytes = new byte[16];
    random.nextBytes(bytes);
    return new BigInteger(bytes).toString();
  }

  public static LogIngestRecord toLogIngestRecord(String svc, LogRecord record) {
    var tsMillis = record.getTimeUnixNano() / 1_000_000L;
    var tid = Hex.encodeHexString(record.getTraceId().toByteArray());
    var level = record.getSeverityNumber().getNumber();
    var body = record.getBody().getStringValue();
    return LogIngestRecord.builder()
        .tsMillis(tsMillis)
        .traceId(tid)
        .service(svc)
        .level(level)
        .body(body)
        .service(svc)
        .build();
  }

  public static BinaryLogRecordV1 toBinaryRecord(LogIngestRecord rec) {
    long tsMillis = rec.tsMillis();
    String traceId = rec.traceId();
    int level = rec.level();
    String body = rec.body();
    String service = rec.service();
    // proto
    var recordBuilder =
        BinaryLogRecordV1.builder()
            .docId(getRandomDocId())
            .tsMillis(tsMillis)
            .level(level)
            .body(body)
            .service(service);

    if (traceId != null && !traceId.isEmpty()) {
      recordBuilder.traceId(traceId);
    }
    return recordBuilder.build();
  }
}
