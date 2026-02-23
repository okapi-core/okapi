package org.okapi.metrics.query;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.primitives.GaugeSketch;
import org.okapi.primitives.ReadOnlySketch;
import org.okapi.primitives.TimestampedReadonlySketch;
import org.okapi.queryproc.FanoutGrouper;
import org.okapi.routing.StreamRouter;

public class PeerMetricsQpTests {

  public static final Long IDX_DUR = 200L;

  @Test
  void testPeerQuerying_singlePeer() throws Exception {
    var router = Mockito.mock(StreamRouter.class);
    var client = Mockito.mock(MetricsClient.class);
    var qp = getQp(router, client);
    // 5 blocks -> give a particular peer all 5
    Mockito.when(router.getNodesForReading(Mockito.any(), Mockito.anyLong()))
        .thenReturn("peer1");
    Mockito.when(
            client.queryGaugeSketches(
                Mockito.eq("peer1"),
                Mockito.eq("metricA"),
                Mockito.eq(Map.of("A", "B")),
                Mockito.eq(RES_TYPE.SECONDLY),
                Mockito.anyLong(),
                Mockito.anyLong()))
        .thenReturn(Arrays.asList(getSketch(100L, 10f, 5f, 2f)));
    var gauges =
        qp.getGaugeSketches( "metricA", Map.of("A", "B"), RES_TYPE.SECONDLY, 0, 1000L);
    Mockito.verify(client, Mockito.times(1))
        .queryGaugeSketches(
            Mockito.eq("peer1"),
            Mockito.eq("metricA"),
            Mockito.eq(Map.of("A", "B")),
            Mockito.eq(RES_TYPE.SECONDLY),
            Mockito.anyLong(),
            Mockito.anyLong());
    Assertions.assertEquals(1, gauges.size());
  }

  public PeerMetricsQp getQp(StreamRouter streamRouter, MetricsClient client) {
    var timeout = Duration.ofSeconds(10);
    var executor = Executors.newFixedThreadPool(4);
    var cfg = Mockito.mock(org.okapi.metrics.config.MetricsCfg.class);
    Mockito.when(cfg.getIdxExpiryDuration()).thenReturn(IDX_DUR);
    return new PeerMetricsQp(cfg, new FanoutGrouper(streamRouter), client, timeout, executor);
  }

  public TimestampedReadonlySketch getSketch(long timestamp, float mean, float count, float dev) {
    var sketch = new ReadOnlySketch(mean, count, dev, sampleSketch());
    return new TimestampedReadonlySketch(timestamp, sketch);
  }

  public byte[] sampleSketch() {
    var gauge = new GaugeSketch();
    return gauge.getWithQuantiles().getFloatsSketch().toByteArray();
  }
}
