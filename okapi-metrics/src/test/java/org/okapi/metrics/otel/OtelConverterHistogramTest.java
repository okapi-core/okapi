package org.okapi.metrics.otel;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.metrics.v1.*;
import io.opentelemetry.proto.resource.v1.Resource;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.MetricType;

public class OtelConverterHistogramTest {

  @Test
  void histogramConversion_basic() {
    // One histogram point with 2 explicit bounds => 3 bucket counts
    HistogramDataPoint hdp =
        HistogramDataPoint.newBuilder()
            .setStartTimeUnixNano(1_000_000L)
            .setTimeUnixNano(4_000_000L)
            .addAllExplicitBounds(List.of(10.0, 20.0))
            .addAllBucketCounts(List.of(5L, 7L, 2L))
            .build();

    Histogram hist =
        Histogram.newBuilder()
            .setAggregationTemporality(
                AggregationTemporality.AGGREGATION_TEMPORALITY_DELTA)
            .addDataPoints(hdp)
            .build();

    Metric m = Metric.newBuilder().setName("latency_ms").setHistogram(hist).build();

    ScopeMetrics sm = ScopeMetrics.newBuilder().addMetrics(m).build();
    ResourceMetrics rm =
        ResourceMetrics.newBuilder().setResource(Resource.newBuilder().build()).addScopeMetrics(sm).build();

    ExportMetricsServiceRequest req =
        ExportMetricsServiceRequest.newBuilder().addResourceMetrics(rm).build();

    OtelConverter c = new OtelConverter();
    List<ExportMetricsRequest> out = c.toOkapiRequests("t1", req);

    assertEquals(1, out.size());
    ExportMetricsRequest r = out.get(0);
    assertEquals(MetricType.HISTO, r.getType());
    assertEquals("latency_ms", r.getMetricName());
    assertNotNull(r.getHisto());
    assertEquals(1, r.getHisto().getHistoPoints().size());

    var hp = r.getHisto().getHistoPoints().get(0);
    assertEquals(1L, hp.getStart());
    assertEquals(4L, hp.getEnd());
    assertArrayEquals(new float[] {10.0f, 20.0f}, hp.getBuckets());
    assertArrayEquals(new int[] {5, 7, 2}, hp.getBucketCounts());
  }
}

