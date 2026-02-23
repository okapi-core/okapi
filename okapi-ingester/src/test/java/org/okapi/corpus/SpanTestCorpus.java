/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.corpus;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.okapi.traces.io.SpanIngestionRecord;
import org.okapi.traces.io.SpanPage;

@AllArgsConstructor
public class SpanTestCorpus {
  long testStartNanos;

  public SpanPage buildTestPage() {
    var spanPage = new SpanPage(10, 0.5, 1000L, 2000L);
    for (var req : getIndividualRecords()) {
      spanPage.append(SpanIngestionRecord.from("default", req));
    }
    return spanPage;
  }

  /** Returns individual ExportTraceServiceRequests to send to Kafka. */
  public List<Span> getIndividualRecords() {
    var nanos = 1_000_000L;
    var r1 =
        buildTestSpan(
            "trace-1",
            "span-1",
            "parent-span-1",
            Map.of("http.method", "GET", "http.url", "/api/resource"),
            Map.of("http.status_code", 200.0),
            testStartNanos + nanos,
            testStartNanos + 2 * nanos);
    var r2 =
        buildTestSpan(
            "trace-1",
            "span-2",
            "parent-span-1",
            Map.of("http.method", "GET", "http.url", "/api/resource"),
            Map.of("http.status_code", 200.0),
            testStartNanos + 4 * nanos,
            testStartNanos + 5 * nanos);
    var r3 =
        buildTestSpan(
            "trace-2",
            "span-2",
            "parent-span-2",
            Map.of("db.system", "mysql"),
            Map.of("db.rows_affected", 5.0),
            testStartNanos + 2 * nanos,
            testStartNanos + 3 * nanos);
    return List.of(r1, r2, r3);
  }

  public Span buildTestSpan(
      String traceId,
      String spanId,
      String parentSpanId,
      Map<String, String> stringAttributes,
      Map<String, Double> doubleAttributes,
      long startTime,
      long endTime) {
    return Span.newBuilder()
        .setTraceId(ByteString.copyFrom(traceId.getBytes()))
        .setSpanId(ByteString.copyFrom(spanId.getBytes()))
        .setParentSpanId(ByteString.copyFrom(parentSpanId.getBytes()))
        .addAllAttributes(
            stringAttributes.entrySet().stream()
                .map(
                    e ->
                        KeyValue.newBuilder()
                            .setKey(e.getKey())
                            .setValue(AnyValue.newBuilder().setStringValue(e.getValue()).build())
                            .build())
                .toList())
        .addAllAttributes(
            doubleAttributes.entrySet().stream()
                .map(
                    e ->
                        KeyValue.newBuilder()
                            .setKey(e.getKey())
                            .setValue(AnyValue.newBuilder().setDoubleValue(e.getValue()).build())
                            .build())
                .toList())
        .setStartTimeUnixNano(startTime)
        .setEndTimeUnixNano(endTime)
        .build();
  }
}
