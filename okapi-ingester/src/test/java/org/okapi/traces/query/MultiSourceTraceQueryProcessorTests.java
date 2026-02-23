/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.query;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.okapi.logs.query.QueryConfig;
import org.okapi.primitives.BinarySpanRecordV2;
import org.okapi.spring.configs.properties.QueryCfg;

public class MultiSourceTraceQueryProcessorTests {

  private static BinarySpanRecordV2 item(String traceId) {
    var span =
        Span.newBuilder()
            .setTraceId(ByteString.copyFrom(traceId.getBytes()))
            .setSpanId(ByteString.copyFrom("span-123".getBytes()))
            .setParentSpanId(ByteString.copyFrom("span-000".getBytes()))
            .setStartTimeUnixNano(1625079600000L * 1_000_000)
            .setEndTimeUnixNano(1625079670000L * 1_000_000)
            .build();
    return new BinarySpanRecordV2(span);
  }

  @Test
  void testMergesFromAllSources() throws Exception {
    var buffer = mock(SpanBufferPoolQueryProcessor.class);
    var disk = mock(OnDiskTraceQueryProcessor.class);
    var s3 = mock(S3TraceQueryProcessor.class);
    var member = mock(PeersTraceQueryProcessor.class);
    when(buffer.getTraces(any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("buf")));
    when(disk.getTraces(any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("disk")));
    when(s3.getTraces(any(), anyLong(), anyLong(), any(), any())).thenReturn(List.of(item("s3")));
    when(member.getTraces(any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("member")));

    var qp = getQueryProcessor(buffer, s3, member, disk);

    var bufResult =
        qp.getTraces(
            "s", 0, 10, new SpanPageTraceFilter("buf".getBytes()), QueryConfig.allSources());
    Assertions.assertEquals(1, bufResult.size());
    var diskResult =
        qp.getTraces(
            "s", 0, 10, new SpanPageTraceFilter("disk".getBytes()), QueryConfig.allSources());
    Assertions.assertEquals(1, diskResult.size());

    var s3Result =
        qp.getTraces(
            "s", 0, 10, new SpanPageTraceFilter("s3".getBytes()), QueryConfig.allSources());
    Assertions.assertEquals(1, s3Result.size());
    var memberResult =
        qp.getTraces(
            "s", 0, 10, new SpanPageTraceFilter("member".getBytes()), QueryConfig.allSources());
    Assertions.assertEquals(1, memberResult.size());
  }

  @Test
  void testDeduplication() throws Exception {
    var buffer = mock(SpanBufferPoolQueryProcessor.class);
    var disk = mock(OnDiskTraceQueryProcessor.class);
    var s3 = mock(S3TraceQueryProcessor.class);
    var member = mock(PeersTraceQueryProcessor.class);
    when(buffer.getTraces(any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("common-trace")));
    when(disk.getTraces(any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("common-trace")));
    when(s3.getTraces(any(), anyLong(), anyLong(), any(), any())).thenReturn(List.of(item("s3")));
    when(member.getTraces(any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("member")));

    var qp = getQueryProcessor(buffer, s3, member, disk);
    var result =
        qp.getTraces(
            "s",
            0,
            10,
            new SpanPageTraceFilter("common-trace".getBytes()),
            QueryConfig.allSources());
    Assertions.assertEquals(1, result.size());
  }

  @Test
  void testFailsOnSingleException() throws Exception {
    var buffer = mock(SpanBufferPoolQueryProcessor.class);
    var disk = mock(OnDiskTraceQueryProcessor.class);
    var s3 = mock(S3TraceQueryProcessor.class);
    var member = mock(PeersTraceQueryProcessor.class);
    when(buffer.getTraces(any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("common-trace")));
    when(disk.getTraces(any(), anyLong(), anyLong(), any(), any())).thenThrow(Exception.class);
    when(s3.getTraces(any(), anyLong(), anyLong(), any(), any())).thenReturn(List.of(item("s3")));
    when(member.getTraces(any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("member")));
    var qp = getQueryProcessor(buffer, s3, member, disk);
    Assertions.assertThrows(
        CompletionException.class,
        () ->
            qp.getTraces(
                "s",
                0,
                10,
                new SpanPageTraceFilter("common-trace".getBytes()),
                QueryConfig.allSources()));
  }

  public MultiSourceTraceQueryProcessor getQueryProcessor(
      SpanBufferPoolQueryProcessor buffer,
      S3TraceQueryProcessor s3,
      PeersTraceQueryProcessor memberSet,
      OnDiskTraceQueryProcessor disk) {
    return new MultiSourceTraceQueryProcessor(buffer, s3, memberSet, disk, getQueryCfg());
  }

  public QueryCfg getQueryCfg() {
    var cfg = new QueryCfg();
    cfg.setLogsQueryProcPoolSize(4);
    cfg.setLogsFanoutPoolSize(2);
    cfg.setMetricsQueryProcPoolSize(4);
    cfg.setMetricsFanoutPoolSize(2);
    cfg.setTracesQueryProcPoolSize(4);
    cfg.setTracesFanoutPoolSize(2);
    return cfg;
  }
}
