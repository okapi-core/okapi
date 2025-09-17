package org.okapi.metrics.cas.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.okapi.metrics.GlobalTestConfig.okapiWait;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.exceptions.BadRequestException;
import org.okapi.fixtures.DeltaSumGenerator;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.cas.CasTesting;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.pojos.SUM_TYPE;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.MetricType;
import org.okapi.rest.metrics.payloads.Sum;
import org.okapi.rest.metrics.payloads.SumPoint;
import org.okapi.rest.metrics.payloads.SumType;

public class CasSumTests extends CasTesting {

  public static final String TENANT = "ten_" + RandomStringUtils.random(5, true, false);
  public static final String SUM_METRIC = "sum";
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
  public void testCounterSingle()
      throws StatisticsFrozenException, BadRequestException, InterruptedException {
    var counterGenerator = new DeltaSumGenerator(10, Duration.of(10, ChronoUnit.SECONDS));
    var end = System.currentTimeMillis();
    var st = end - Duration.of(1, ChronoUnit.HOURS).toMillis();
    counterGenerator.generate(st, end);

    var readings = counterGenerator.getReadings();
    var request =
        ExportMetricsRequest.builder()
            .tenantId(TENANT)
            .metricName(SUM_METRIC)
            .tags(TAGS)
            .type(MetricType.COUNTER)
            .sum(
                Sum.builder()
                    .sumType(SumType.DELTA)
                    .sumPoints(
                        readings.stream()
                            .map(r -> new SumPoint(r.start(), r.end(), r.count()))
                            .toList())
                    .build())
            .build();
    var writer = resourceFactory.casMetricsWriter(getMapper(), node);
    var reader = resourceFactory.casTsReaders(getMapper(), node);
    writer.onRequestArrive(request);
    var qWindow = Duration.of(10, ChronoUnit.SECONDS);
    var agg = counterGenerator.aggregate(st, end, qWindow);
    var path = MetricPaths.univPath(TENANT, SUM_METRIC, TAGS);
    okapiWait()
        .until(
            () -> {
              var canary = reader.scanSum(path, st, end, qWindow.toMillis(), SUM_TYPE.DELTA);
              return canary.getTs().size() == agg.size();
            });
    var result = reader.scanSum(path, st, end, qWindow.toMillis(), SUM_TYPE.DELTA);
    var expectedTs = agg.stream().map(a -> 1000 * (a.start() / 1000)).toList();
    var expectedVals = agg.stream().map(a -> a.count()).toList();
    assertEquals(expectedTs, result.getTs());
    assertEquals(expectedVals, result.getCounts());
  }
}
