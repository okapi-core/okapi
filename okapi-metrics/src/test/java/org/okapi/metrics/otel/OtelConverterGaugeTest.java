package org.okapi.metrics.otel;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Gauge;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.proto.resource.v1.Resource;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.MetricType;

public class OtelConverterGaugeTest {

  @Test
  void gaugeConversion_basic() {
    // Resource tag
    KeyValue resTag =
        KeyValue.newBuilder()
            .setKey("service.name")
            .setValue(AnyValue.newBuilder().setStringValue("svc").build())
            .build();

    // Two gauge points with per-point attribute
    NumberDataPoint p1 =
        NumberDataPoint.newBuilder()
            .setTimeUnixNano(1_000_000L) // 1 ms
            .setAsDouble(1.5)
            .addAttributes(
                KeyValue.newBuilder()
                    .setKey("pod")
                    .setValue(AnyValue.newBuilder().setStringValue("p1").build())
                    .build())
            .build();
    NumberDataPoint p2 =
        NumberDataPoint.newBuilder()
            .setTimeUnixNano(2_000_000L) // 2 ms
            .setAsInt(2)
            .addAttributes(
                KeyValue.newBuilder()
                    .setKey("pod")
                    .setValue(AnyValue.newBuilder().setStringValue("p1").build())
                    .build())
            .build();

    Gauge gauge = Gauge.newBuilder().addDataPoints(p1).addDataPoints(p2).build();

    Metric m = Metric.newBuilder().setName("cpu_usage").setGauge(gauge).build();

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
    assertEquals("t1", r.getTenantId());
    assertEquals("cpu_usage", r.getMetricName());
    assertEquals(MetricType.GAUGE, r.getType());
    assertTrue(r.getTags().containsKey("service.name"));
    assertEquals("svc", r.getTags().get("service.name"));
    assertEquals("p1", r.getTags().get("pod"));

    var g = r.getGauge();
    assertNotNull(g);
    assertArrayEquals(new long[] {1, 2}, g.getTs());
    assertEquals(2, g.getValue().length);
    assertEquals(1.5f, g.getValue()[0]);
    assertEquals(2.0f, g.getValue()[1]);
  }
}
