package org.okapi.metrics.cas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.okapi.metrics.GlobalTestConfig.okapiWait;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.exceptions.BadRequestException;
import org.okapi.fixtures.DeltaSumGenerator;
import org.okapi.fixtures.GaugeGenerator;
import org.okapi.fixtures.HistoGenerator;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.cas.it.CasGaugeTests;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.promql.eval.VectorData;
import org.okapi.promql.parse.LabelMatcher;
import org.okapi.promql.parse.LabelOp;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.MetricType;
import org.okapi.rest.metrics.payloads.Gauge;
import org.okapi.rest.metrics.payloads.Histo;
import org.okapi.rest.metrics.payloads.HistoPoint;
import org.okapi.rest.metrics.payloads.Sum;
import org.okapi.rest.metrics.payloads.SumPoint;
import org.okapi.rest.metrics.payloads.SumType;

public class CasPromQlTests extends CasTesting {

  private static final String TENANT = "ten_" + RandomStringUtils.random(5, true, false);
  private static final String METRIC_A = "metricA"; // gauge
  private static final String METRIC_B = "metricB"; // histo
  private static final String METRIC_C = "metricC"; // counter

  TestResourceFactory resourceFactory;
  Node node = new Node(CasGaugeTests.class.getSimpleName(), "localhost:1001", NodeState.METRICS_CONSUMPTION_START);

  long now;
  long start;
  long step;

  @BeforeEach
  public void setup() throws StatisticsFrozenException, BadRequestException, InterruptedException {
    bootstrap();
    resourceFactory = new TestResourceFactory();

    step = Duration.of(10, ChronoUnit.MINUTES).toMillis();
    now = 60_000 * (System.currentTimeMillis() / 60_000);
    start = now - Duration.of(30, ChronoUnit.MINUTES).toMillis();

    // Generators
    var gaugeGen1 = new GaugeGenerator(Duration.ofSeconds(1), 10);
    var gaugeGen2 = new GaugeGenerator(Duration.ofSeconds(1), 10);
    var histoBuckets = Arrays.asList(10f, 50f, 100f);
    var histoGen = new HistoGenerator(histoBuckets, 10, Duration.ofSeconds(20));
    var counterGen = new DeltaSumGenerator(10, Duration.ofMinutes(1));

    // Generate time ranges staggered across minutes (as in other tests)
    histoGen.generate(start, start + step);
    gaugeGen1.populateRandom(start + step, 0.f, 10.f);
    gaugeGen2.populateRandom(start + step, 0.1f, 2.f);
    counterGen.generate(start + 2 * step, now);

    // Build requests
    var writer = resourceFactory.casMetricsWriter(getMapper(), node);

    var gaugeReqI1 =
        ExportMetricsRequest.builder()
            .tenantId(TENANT)
            .metricName(METRIC_A)
            .type(MetricType.GAUGE)
            .tags(Map.of("job", "api", "instance", "i1"))
            .gauge(
                Gauge.builder()
                    .ts(Longs.toArray(gaugeGen1.getTimestamps()))
                    .value(Floats.toArray(gaugeGen1.getValues()))
                    .build())
            .build();

    var gaugeReqI2 =
        ExportMetricsRequest.builder()
            .tenantId(TENANT)
            .metricName(METRIC_A)
            .type(MetricType.GAUGE)
            .tags(Map.of("job", "api", "instance", "i2"))
            .gauge(
                Gauge.builder()
                    .ts(Longs.toArray(gaugeGen2.getTimestamps()))
                    .value(Floats.toArray(gaugeGen2.getValues()))
                    .build())
            .build();

    var histoReq =
        ExportMetricsRequest.builder()
            .tenantId(TENANT)
            .metricName(METRIC_B)
            .type(MetricType.HISTO)
            .tags(Map.of("job", "web", "env", "prod"))
            .histo(
                Histo.builder()
                    .histoPoints(
                        histoGen.getReadings().stream()
                            .map(r -> new HistoPoint(r.start(), r.end(), Floats.toArray(histoBuckets), Ints.toArray(r.counts())))
                            .toList())
                    .build())
            .build();

    var sumReq =
        ExportMetricsRequest.builder()
            .tenantId(TENANT)
            .metricName(METRIC_C)
            .type(MetricType.COUNTER)
            .tags(Map.of("job", "batch", "env", "stg"))
            .sum(
                Sum.builder()
                    .sumType(SumType.DELTA)
                    .sumPoints(
                        counterGen.getReadings().stream()
                            .map(r -> new SumPoint(r.start(), r.end(), r.count()))
                            .toList())
                    .build())
            .build();

    // Ingest data
    writer.onRequestArrive(histoReq);
    writer.onRequestArrive(gaugeReqI1);
    writer.onRequestArrive(gaugeReqI2);
    writer.onRequestArrive(sumReq);

    // Wait until hints are visible; probe TsSearcher counts
    var searcher = resourceFactory.casTsSearcher(getMapper(), node);
    okapiWait().until(() -> searcher.search(TENANT, "*", start, now).size() >= 3);
  }

  @Test
  public void expand_matchesByLabelsAndRegex() {
    var searcher = resourceFactory.casTsSearcher(getMapper(), node);
    var discovery = new CasSeriesDiscovery(TENANT, searcher);

    long st = start;
    long en = now;

    // Simple equals label match (metricA with job=api)
    var res1 = discovery.expand(METRIC_A, List.of(new LabelMatcher("job", LabelOp.EQ, "api")), st, en);
    assertEquals(2, res1.size());

    // Regex on instance
    var res2 = discovery.expand(METRIC_A, List.of(new LabelMatcher("instance", LabelOp.RE, "i[12]")), st, en);
    assertEquals(2, res2.size());

    // Negative regex to exclude env=prod
    var res3 = discovery.expand(null, List.of(new LabelMatcher("job", LabelOp.EQ, "web"), new LabelMatcher("env", LabelOp.NRE, "prod")), st, en);
    assertEquals(0, res3.size());

    // __name__ matching via label
    var res4 = discovery.expand(null, List.of(new LabelMatcher("__name__", LabelOp.EQ, METRIC_B), new LabelMatcher("env", LabelOp.EQ, "prod")), st, en);
    assertEquals(1, res4.size());
    VectorData.SeriesId id = res4.get(0);
    assertEquals(METRIC_B, id.metric());
  }
}
