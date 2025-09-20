package org.okapi.metrics.cas.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.okapi.metrics.GlobalTestConfig.okapiWait;

import com.google.api.client.util.Lists;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Longs;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.exceptions.BadRequestException;
import org.okapi.fixtures.GaugeGenerator;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.cas.CasTesting;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.MetricType;
import org.okapi.rest.metrics.payloads.Gauge;

@Slf4j
public class CasGaugeTests extends CasTesting {

  public static final String TENANT = "ten_" + RandomStringUtils.random(5, true, false);
  public static final String GAUGE_METRIC = "gauge";
  public static final Map<String, String> TAGS = Map.of("tag", "value");

  TestResourceFactory resourceFactory;
  Node node =
      new Node(
          CasGaugeTests.class.getSimpleName(),
          "localhost:1001",
          NodeState.METRICS_CONSUMPTION_START);

  @BeforeEach
  public void setup() {
    bootstrap();
    resourceFactory = new TestResourceFactory();
  }

  @Test
  public void testGaugeSecondly()
      throws StatisticsFrozenException, BadRequestException, InterruptedException {
    var gen = new GaugeGenerator(Duration.of(100, ChronoUnit.MILLIS), 1).populateRandom(0.f, 100.f);
    var writer = resourceFactory.casMetricsWriter(getMapper(), node);
    var request =
        ExportMetricsRequest.builder()
            .gauge(
                Gauge.builder()
                    .ts(Longs.toArray(gen.getTimestamps()))
                    .value(Floats.toArray(gen.getValues()))
                    .build())
            .metricName(GAUGE_METRIC)
            .type(MetricType.GAUGE)
            .tags(TAGS)
            .tenantId(TENANT)
            .build();
    writer.onRequestArrive(request);

    var reader = resourceFactory.casTsReaders(getMapper(), node);
    var univPath = MetricPaths.univPath(TENANT, GAUGE_METRIC, TAGS);
    var reduction = gen.avgReduction(RES_TYPE.SECONDLY);
    var st = gen.getTimestamps().stream().reduce(Math::min).get();
    var en = gen.getTimestamps().stream().reduce(Math::max).get();
    var expectedTs = reduction.getTimestamp();
    var expectedVals = reduction.getValues();
    okapiWait()
        .until(
            () -> {
              var canaryResult = reader.scanGauageSecondlyRes(univPath, st, en);
              var canaryTs = Lists.newArrayList(canaryResult.keySet());
              return canaryTs.size() == reduction.getTimestamp().size();
            });

    var result = reader.scanGauageSecondlyRes(univPath, st, en);
    var ts = Lists.newArrayList(result.keySet());
    var values = result.values().stream().map(res -> res.avg()).toList();
    assertEquals(expectedTs, ts);
    assertEquals(expectedVals, values);
  }

  @Test
  public void testGaugeMinutely()
      throws StatisticsFrozenException, BadRequestException, InterruptedException {
    var gen = new GaugeGenerator(Duration.of(100, ChronoUnit.MILLIS), 5).populateRandom(0.f, 100.f);
    var writer = resourceFactory.casMetricsWriter(getMapper(), node);
    var request =
        ExportMetricsRequest.builder()
            .gauge(
                Gauge.builder()
                    .ts(Longs.toArray(gen.getTimestamps()))
                    .value(Floats.toArray(gen.getValues()))
                    .build())
            .metricName(GAUGE_METRIC)
            .type(MetricType.GAUGE)
            .tags(TAGS)
            .tenantId(TENANT)
            .build();
    writer.onRequestArrive(request);

    var reader = resourceFactory.casTsReaders(getMapper(), node);
    var univPath = MetricPaths.univPath(TENANT, GAUGE_METRIC, TAGS);
    var reduction = gen.avgReduction(RES_TYPE.MINUTELY);
    var st = gen.getTimestamps().stream().reduce(Math::min).get();
    var en = gen.getTimestamps().stream().reduce(Math::max).get();
    var expectedTs = reduction.getTimestamp();
    var expectedVals = reduction.getValues();
    okapiWait()
        .until(
            () -> {
              var canaryResult = reader.scanGauageMinutelyRes(univPath, st, en);
              var canaryTs = Lists.newArrayList(canaryResult.keySet());
              return canaryTs.size() == reduction.getTimestamp().size();
            });

    var result = reader.scanGauageMinutelyRes(univPath, st, en);
    var ts = Lists.newArrayList(result.keySet());
    var values = result.values().stream().map(res -> res.avg()).toList();
    assertEquals(expectedTs, ts);
    assertEquals(expectedVals, values);
  }

  @Test
  public void testGaugeHourly()
      throws StatisticsFrozenException, BadRequestException, InterruptedException {
    var gen =
        new GaugeGenerator(Duration.of(10, ChronoUnit.SECONDS), 60 * 6).populateRandom(0.f, 100.f);
    var writer = resourceFactory.casMetricsWriter(getMapper(), node);
    var request =
        ExportMetricsRequest.builder()
            .gauge(
                Gauge.builder()
                    .ts(Longs.toArray(gen.getTimestamps()))
                    .value(Floats.toArray(gen.getValues()))
                    .build())
            .metricName(GAUGE_METRIC)
            .type(MetricType.GAUGE)
            .tags(TAGS)
            .tenantId(TENANT)
            .build();
    writer.onRequestArrive(request);

    var reader = resourceFactory.casTsReaders(getMapper(), node);
    var univPath = MetricPaths.univPath(TENANT, GAUGE_METRIC, TAGS);
    var reduction = gen.avgReduction(RES_TYPE.HOURLY);
    var st = gen.getTimestamps().stream().reduce(Math::min).get();
    var en = gen.getTimestamps().stream().reduce(Math::max).get();
    var expectedTs = reduction.getTimestamp();
    var expectedVals = reduction.getValues();
    okapiWait()
        .until(
            () -> {
              var canaryResult = reader.scanGauageHourlyRes(univPath, st, en);
              var canaryTs = Lists.newArrayList(canaryResult.keySet());
              log.info("Canary ts: " + canaryTs);
              return canaryTs.size() == reduction.getTimestamp().size();
            });

    var result = reader.scanGauageHourlyRes(univPath, st, en);
    var ts = Lists.newArrayList(result.keySet());
    var values = result.values().stream().map(res -> res.avg()).toList();
    assertEquals(expectedTs, ts);
    assertEquals(expectedVals, values);
  }
}
