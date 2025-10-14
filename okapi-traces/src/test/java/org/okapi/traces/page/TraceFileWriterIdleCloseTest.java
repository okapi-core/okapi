package org.okapi.traces.page;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

public class TraceFileWriterIdleCloseTest {
  private static byte[] hex(String h) {
    return HexFormat.of().parseHex(h);
  }

  private static ExportTraceServiceRequest payload(long startMs, long endMs) {
    Span sp =
        Span.newBuilder()
            .setTraceId(
                com.google.protobuf.ByteString.copyFrom(hex("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")))
            .setSpanId(com.google.protobuf.ByteString.copyFrom(hex("1111111111111111")))
            .setStartTimeUnixNano(startMs * 1_000_000L)
            .setEndTimeUnixNano(endMs * 1_000_000L)
            .build();
    ScopeSpans scope = ScopeSpans.newBuilder().addSpans(sp).build();
    ResourceSpans rs = ResourceSpans.newBuilder().addScopeSpans(scope).build();
    return ExportTraceServiceRequest.newBuilder().addResourceSpans(rs).build();
  }

  @Test
  void idleClose_sweepsOpenStreams() throws Exception {
    Path dir = Files.createTempDirectory("okapi-writer-test");
    try {
      TraceWriterConfig cfg =
          TraceWriterConfig.builder().idleCloseMillis(1).reapIntervalMillis(1).build();
      TraceFileWriter writer = new TraceFileWriter(dir, cfg, null);
      var page = SpanPage.newEmpty(1000, 0.01);
      page.append(payload(1700000000000L, 1700000000100L));
      writer.write("t", "a", page);
      assertTrue(writer.openStreamCount() > 0);
      writer.sweepNow();
      // after sweep, idle streams should be closed
      assertEquals(0, writer.openStreamCount());
      writer.close();
    } finally {
      Files.walk(dir)
          .sorted((a, b) -> b.getNameCount() - a.getNameCount())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
              });
    }
  }
}
