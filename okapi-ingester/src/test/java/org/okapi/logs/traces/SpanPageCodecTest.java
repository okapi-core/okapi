/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.traces;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;
import org.okapi.traces.io.SpanIngestionRecord;
import org.okapi.traces.io.SpanPage;
import org.okapi.traces.io.SpanPageCodec;

public class SpanPageCodecTest {

  private static Span payload(byte[] traceId, byte[] spanId, long startMs, long endMs) {
    Span sp =
        Span.newBuilder()
            .setTraceId(ByteString.copyFrom(traceId))
            .setSpanId(ByteString.copyFrom(spanId))
            .setStartTimeUnixNano(startMs * 1_000_000L)
            .setEndTimeUnixNano(endMs * 1_000_000L)
            .build();
    return sp;
  }

  String SVC_A = "svcA";

  @Test
  void roundTrip_and_isFull_thresholds() throws Exception {
    long base = 1700000000000L;
    SpanPage page = new SpanPage(1000, 0.01, 1000, 30);
    page.append(
        SpanIngestionRecord.from(
            SVC_A, payload("trace-a".getBytes(), "span-a".getBytes(), base, base + 10)));
    page.append(
        SpanIngestionRecord.from(
            SVC_A, payload("trace-b".getBytes(), "span-b".getBytes(), base + 20, base + 30)));

    var codec = new SpanPageCodec();
    byte[] bytes = codec.serialize(page);
    var restored = codec.deserialize(bytes);
    assertTrue(restored.isPresent());
    assertEquals(page.getPageBody().size(), restored.get().getPageBody().size());
    assertTrue(page.isFull());
    assertTrue(
        page.getPageBody().snapshot().stream()
            .anyMatch(
                s -> Arrays.equals("trace-a".getBytes(), s.getSpan().getTraceId().toByteArray())));
    assertTrue(
        page.getPageBody().snapshot().stream()
            .anyMatch(
                s -> Arrays.equals("span-a".getBytes(), s.getSpan().getSpanId().toByteArray())));
    assertTrue(
        page.getPageBody().snapshot().stream()
            .anyMatch(
                s -> Arrays.equals("trace-b".getBytes(), s.getSpan().getTraceId().toByteArray())));
    assertTrue(
        page.getPageBody().snapshot().stream()
            .anyMatch(
                s -> Arrays.equals("span-b".getBytes(), s.getSpan().getSpanId().toByteArray())));
    assertTrue(page.getMetadata().maybeContainsTraceId("trace-a".getBytes()));
    assertTrue(page.getMetadata().maybeContainsTraceId("trace-b".getBytes()));
    assertFalse(page.getMetadata().maybeContainsTraceId("trace-c".getBytes()));
  }

  @Test
  void testRepeats() throws IOException, StreamReadingException, NotEnoughBytesException {
    long base = 1700000000000L;
    SpanPage page = new SpanPage(1000, 0.01, 1000, 30);
    var payload = payload("trace-a".getBytes(), "span-a".getBytes(), base, base + 10);
    page.append(SpanIngestionRecord.from(SVC_A, payload));
    page.append(SpanIngestionRecord.from(SVC_A, payload));

    var codec = new SpanPageCodec();
    byte[] bytes = codec.serialize(page);
    var restored = codec.deserialize(bytes);
    assertTrue(restored.isPresent());
    assertEquals(page.getPageBody().size(), restored.get().getPageBody().size());
    assertTrue(page.isFull());
    assertTrue(page.getMetadata().maybeContainsTraceId("trace-a".getBytes()));
    assertFalse(page.getMetadata().maybeContainsTraceId("trace-c".getBytes()));
    assertTrue(
        page.getPageBody().snapshot().stream()
            .anyMatch(
                s -> Arrays.equals("trace-a".getBytes(), s.getSpan().getTraceId().toByteArray())));
    assertTrue(
        page.getPageBody().snapshot().stream()
            .anyMatch(
                s -> Arrays.equals("span-a".getBytes(), s.getSpan().getSpanId().toByteArray())));
  }
}
