package org.okapi.logs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.okapi.logs.TestApplication;
import org.okapi.logs.runtime.LogPageBufferPool;
import org.okapi.swim.membership.MembershipEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("test")
class OtelLogsControllerBulkIngestTest {

  @Autowired OtelLogsController controller;
  @Autowired LogPageBufferPool bufferPool;
  @MockitoBean
  MembershipEventPublisher membershipEventPublisher;

  @Test
  void bulkIngest_writesRecordsToBufferPool() throws Exception {
    String tenant = "t-bulk";
    String stream = "s-bulk";
    long nowNs = Instant.now().toEpochMilli() * 1_000_000L;

    LogRecord lr1 =
        LogRecord.newBuilder()
            .setTimeUnixNano(nowNs)
            .setSeverityNumber(SeverityNumber.forNumber(9))
            .setTraceId(ByteString.copyFrom(new byte[0]))
            .setBody(
                io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                    .setStringValue("bulk one")
                    .build())
            .build();
    LogRecord lr2 =
        LogRecord.newBuilder()
            .setTimeUnixNano(nowNs + 1)
            .setSeverityNumber(SeverityNumber.forNumber(13))
            .setTraceId(ByteString.copyFrom(new byte[0]))
            .setBody(
                io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                    .setStringValue("bulk two")
                    .build())
            .build();

    ScopeLogs scope = ScopeLogs.newBuilder().addLogRecords(lr1).addLogRecords(lr2).build();

    controller.bulkIngest(tenant, stream, scope.toByteArray());

    var page = bufferPool.snapshotActivePage(tenant, stream);
    assertNotNull(page);
    assertEquals(2, page.sizeInDocs());
  }
}

