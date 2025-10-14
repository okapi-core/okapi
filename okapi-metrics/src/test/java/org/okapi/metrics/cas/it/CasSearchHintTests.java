package org.okapi.metrics.cas.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.okapi.metrics.cas.it.CasGaugeTests.GAUGE_METRIC;
import static org.okapi.metrics.cas.it.CasHistoTests.HISTO_METRIC;
import static org.okapi.metrics.cas.it.CasSumTests.SUM_METRIC;

import com.google.common.collect.Sets;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.exceptions.BadRequestException;
import org.okapi.fixtures.DeltaSumGenerator;
import org.okapi.fixtures.GaugeGenerator;
import org.okapi.fixtures.HistoGenerator;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.cas.CasTesting;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.fdb.MetricTypesSearchVals;
import org.okapi.metrics.rollup.SearchResult;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.MetricType;
import org.okapi.rest.metrics.payloads.*;

@Slf4j
public class CasSearchHintTests extends CasTesting {

  public static final String TENANT = "ten_" + RandomStringUtils.random(5, true, false);
  public static final String APP = "app";
  public static final Map<String, String> TAGS = Map.of("tag", "value");

  TestResourceFactory resourceFactory;
  Node node =
      new Node(
          CasGaugeTests.class.getSimpleName(),
          "localhost:1001",
          NodeState.METRICS_CONSUMPTION_START);
  Long now;
  Long start;
  Long step;

  HistoGenerator histoGen;
  DeltaSumGenerator counterGen;
  GaugeGenerator gaugeGen;

  @BeforeEach
  public void setup() throws StatisticsFrozenException, BadRequestException, InterruptedException {
    bootstrap();
    resourceFactory = new TestResourceFactory();

    step = Duration.of(10, ChronoUnit.MINUTES).toMillis();
    var buckets = Arrays.asList(10.f, 20.f, 40.f, 100.f);
    /**
     * Test case setup: HistoGram -> t, t - 10min ; Gauge -> t - 10min, t - 20min; Counter -> t -
     * 20min, t - 30min
     */
    /**
     * Generate the data using wither HistoGenerator, DeltaSumGenerator or GaugeGenerator Persist by
     * wrapping the data into
     */
    now = 60_000 * (System.currentTimeMillis() / 60_000);
    start = now - Duration.of(30, ChronoUnit.MINUTES).toMillis();
    /** wire up the generators */
    histoGen = new HistoGenerator(buckets, 10, Duration.of(20, ChronoUnit.SECONDS));
    counterGen = new DeltaSumGenerator(10, Duration.of(1, ChronoUnit.MINUTES));
    gaugeGen =
        new GaugeGenerator(
            Duration.of(1, ChronoUnit.SECONDS), 10); // 10 min interval sampled at 1 second
    histoGen.generate(start, start + step);
    gaugeGen.populateRandom(start + step, start + 2 * step);
    counterGen.generate(start + 2 * step, now);

    /** get the readings, gauge already has reading */
    var histos = histoGen.getReadings();
    var counts = counterGen.getReadings();

    /** Convert to reuest */
    var histoReq =
        ExportMetricsRequest.builder()
            .tenantId(TENANT)
            .metricName(HISTO_METRIC)
            .tags(TAGS)
            .type(MetricType.HISTO)
            .histo(
                Histo.builder()
                    .histoPoints(
                        histos.stream()
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

    var gaugeReq =
        ExportMetricsRequest.builder()
            .gauge(
                Gauge.builder()
                    .ts(Longs.toArray(gaugeGen.getTimestamps()))
                    .value(Floats.toArray(gaugeGen.getValues()))
                    .build())
            .metricName(GAUGE_METRIC)
            .type(MetricType.GAUGE)
            .tags(TAGS)
            .tenantId(TENANT)
            .build();

    var sumReq =
        ExportMetricsRequest.builder()
            .tenantId(TENANT)
            .metricName(SUM_METRIC)
            .tags(TAGS)
            .type(MetricType.COUNTER)
            .sum(
                Sum.builder()
                    .sumType(SumType.DELTA)
                    .sumPoints(
                        counts.stream()
                            .map(r -> new SumPoint(r.start(), r.end(), r.count()))
                            .toList())
                    .build())
            .build();

    /** persist to Cas */
    var writer = resourceFactory.casMetricsWriter(getMapper(), node);
    writer.onRequestArrive(histoReq);
    writer.onRequestArrive(gaugeReq);
    writer.onRequestArrive(sumReq);

    /** For debugging */
    log.info("Tenant: {}", TENANT);
    log.info("time: {}, minute block: {}", start, start / (60_000));
  }

  @Test
  public void listAll() {
    var pattern = "*{tag=value}";
    var searcher = resourceFactory.casTsSearcher(getMapper(), node);
    // check the data.
    var all = searcher.search(TENANT, pattern, start, now);
    assertEquals(3, all.size());
    var types = all.stream().map(SearchResult::getType).collect(Collectors.toSet());
    assertEquals(
        Sets.newHashSet(
            MetricTypesSearchVals.SUM, MetricTypesSearchVals.GAUGE, MetricTypesSearchVals.HISTO),
        types);
  }

  @Test
  public void twoResults() {
    var pattern = "*{tag=value}";
    var searcher = resourceFactory.casTsSearcher(getMapper(), node);
    var result = searcher.search(TENANT, pattern, start, start + 2 * step);
    assertEquals(2, result.size());
  }

  @Test
  public void singleResul() {
    var pattern = "*{tag=value}";
    var searcher = resourceFactory.casTsSearcher(getMapper(), node);
    var result = searcher.search(TENANT, pattern, start, start + step);
    assertEquals(1, result.size());
  }

  @Test
  public void noResults() {
    var pattern = "*{tag=value2}";
    var searcher = resourceFactory.casTsSearcher(getMapper(), node);
    var result = searcher.search(TENANT, pattern, start, start + 2 * step);
    assertEquals(0, result.size());
  }
}
