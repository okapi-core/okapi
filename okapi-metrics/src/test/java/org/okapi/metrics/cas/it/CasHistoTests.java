package org.okapi.metrics.cas.it;

import static org.okapi.metrics.GlobalTestConfig.okapiWait;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.exceptions.BadRequestException;
import org.okapi.fixtures.HistoGenerator;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.cas.CasTesting;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.MetricType;
import org.okapi.rest.metrics.payloads.Histo;
import org.okapi.rest.metrics.payloads.HistoPoint;

public class CasHistoTests extends CasTesting {

  public static final String TENANT = "ten_" + RandomStringUtils.random(5, true, false);
  public static final String HISTO_METRIC = "histo";
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
  public void testHisto()
      throws StatisticsFrozenException, BadRequestException, InterruptedException {
    var buckets = Arrays.asList(10.f, 20.f, 40.f, 100.f);
    var histoGenerator = new HistoGenerator(buckets, 10, Duration.of(20, ChronoUnit.SECONDS));
    var en = System.currentTimeMillis();
    var st = System.currentTimeMillis() - Duration.of(1, ChronoUnit.HOURS).toMillis();
    histoGenerator.generate(st, en);
    var readings = histoGenerator.getReadings();

    var request =
        ExportMetricsRequest.builder()
            .tenantId(TENANT)
            .metricName(HISTO_METRIC)
            .tags(TAGS)
            .type(MetricType.HISTO)
            .histo(
                Histo.builder()
                    .histoPoints(
                        readings.stream()
                            .map(
                                r ->
                                    new HistoPoint(
                                        r.start(),
                                        r.end(),
                                        Floats.toArray(buckets),
                                        Ints.toArray(r.counts())))
                            .toList())
                    .build())
            .build();

    var writer = resourceFactory.casMetricsWriter(getMapper(), node);
    var reader = resourceFactory.casTsReaders(getMapper(), node);
    writer.onRequestArrive(request);
    var agg = histoGenerator.aggregate(st, en);
    var path = MetricPaths.univPath(TENANT, HISTO_METRIC, TAGS);
    okapiWait()
        .until(
            () -> {
              var canary = reader.scanHisto(path, st, en);
              return canary.getCounts().equals(agg.counts());
            });
  }
}
