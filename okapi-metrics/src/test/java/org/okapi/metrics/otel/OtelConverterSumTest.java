package org.okapi.metrics.otel;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.*;
import io.opentelemetry.proto.resource.v1.Resource;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.MetricType;
import org.okapi.rest.metrics.payloads.SumType;

public class OtelConverterSumTest {

  @Test
  void sumConversion_delta() {
    // Resource tag
    KeyValue resTag =
        KeyValue.newBuilder()
            .setKey("service.name")
            .setValue(AnyValue.newBuilder().setStringValue("svc").build())
            .build();

    NumberDataPoint p1 =
        NumberDataPoint.newBuilder()
            .setStartTimeUnixNano(1_000_000L)
            .setTimeUnixNano(2_000_000L)
            .setAsInt(3)
            .build();
    NumberDataPoint p2 =
        NumberDataPoint.newBuilder()
            .setStartTimeUnixNano(2_000_000L)
            .setTimeUnixNano(3_000_000L)
            .setAsDouble(4.2)
            .build();

    Sum sum =
        Sum.newBuilder()
            .setAggregationTemporality(AggregationTemporality.AGGREGATION_TEMPORALITY_DELTA)
            .addAllDataPoints(List.of(p1, p2))
            .build();

    Metric m = Metric.newBuilder().setName("requests").setSum(sum).build();
    ScopeMetrics sm = ScopeMetrics.newBuilder().addMetrics(m).build();
    ResourceMetrics rm =
        ResourceMetrics.newBuilder()
            .setResource(Resource.newBuilder().addAttributes(resTag).build())
            .addScopeMetrics(sm)
            .build();

    ExportMetricsServiceRequest req =
        ExportMetricsServiceRequest.newBuilder().addResourceMetrics(rm).build();

    OtelConverter c = new OtelConverter();
    List<ExportMetricsRequest> out = c.toOkapiRequests("t1", req);

    assertEquals(1, out.size());
    ExportMetricsRequest r = out.get(0);
    assertEquals(MetricType.COUNTER, r.getType());
    assertEquals("requests", r.getMetricName());
    assertNotNull(r.getSum());
    assertEquals(SumType.DELTA, r.getSum().getSumType());
    assertEquals(2, r.getSum().getSumPoints().size());

    var sp1 = r.getSum().getSumPoints().get(0);
    assertEquals(1L, sp1.getStart());
    assertEquals(2L, sp1.getEnd());
    assertEquals(3, sp1.getSum());

    var sp2 = r.getSum().getSumPoints().get(1);
    assertEquals(2L, sp2.getStart());
    assertEquals(3L, sp2.getEnd());
    assertEquals(4, sp2.getSum()); // 4.2 rounded -> 4
  }
}

