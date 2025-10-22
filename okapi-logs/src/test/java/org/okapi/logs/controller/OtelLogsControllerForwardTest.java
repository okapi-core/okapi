package org.okapi.logs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.logs.TestApplication;
import org.okapi.logs.forwarding.HttpLogForwarder;
import org.okapi.swim.ping.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("test")
class OtelLogsControllerForwardTest {

  @Autowired HttpLogForwarder forwarder;

  private MockWebServer server;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void forward_postsScopeLogsWithHeaders_andCountsSuccess() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200));

    String tenant = "t-forward";
    String stream = "s-forward";
    long nowNs = Instant.now().toEpochMilli() * 1_000_000L;
    LogRecord lr1 =
        LogRecord.newBuilder()
            .setTimeUnixNano(nowNs)
            .setSeverityNumber(SeverityNumber.forNumber(9))
            .setBody(
                io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                    .setStringValue("hello world")
                    .build())
            .build();
    LogRecord lr2 =
        LogRecord.newBuilder()
            .setTimeUnixNano(nowNs + 1)
            .setSeverityNumber(SeverityNumber.forNumber(13))
            .setBody(
                io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                    .setStringValue("warn msg")
                    .build())
            .build();

    Member remote = new Member("node-1", server.getHostName(), server.getPort());

    forwarder.forward(tenant, stream, remote, List.of(lr1, lr2));

    var recorded = server.takeRequest();
    assertEquals("/v1/logs/bulk", recorded.getPath());
    assertEquals(tenant, recorded.getHeader("X-Okapi-Tenant-Id"));
    assertEquals(stream, recorded.getHeader("X-Okapi-Log-Stream"));

    byte[] body = recorded.getBody().readByteArray();
    ScopeLogs parsed = ScopeLogs.parseFrom(body);
    assertNotNull(parsed);
    assertEquals(2, parsed.getLogRecordsCount());
  }
}
