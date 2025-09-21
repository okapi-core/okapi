package org.okapi.metrics.cas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.okapi.metrics.GlobalTestConfig.okapiWait;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.exceptions.BadRequestException;
import org.okapi.fixtures.DeltaSumGenerator;
import org.okapi.fixtures.GaugeGenerator;
import org.okapi.fixtures.HistoGenerator;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.cas.dao.MetricsMapper;
import org.okapi.metrics.cas.dao.TypeHintsDao;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.pojos.SUM_TYPE;
import org.okapi.metrics.pojos.results.GaugeScan;
import org.okapi.metrics.pojos.results.HistoScan;
import org.okapi.metrics.pojos.results.SumScan;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.promql.eval.ts.RESOLUTION;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.MetricType;
import org.okapi.rest.metrics.payloads.Gauge;
import org.okapi.rest.metrics.payloads.Histo;
import org.okapi.rest.metrics.payloads.HistoPoint;
import org.okapi.rest.metrics.payloads.Sum;
import org.okapi.rest.metrics.payloads.SumPoint;
import org.okapi.rest.metrics.payloads.SumType;

@Slf4j
public class CasTsClientTests extends CasTesting {

  private static String TENANT = "ten_" + RandomStringUtils.random(5, true, false);
  private static final String GAUGE_METRIC = "g_m";
  private static final String HISTO_METRIC = "h_m";
  private static final String COUNTER_METRIC = "c_m";
  private static final Map<String, String> GAUGE_TAGS = Map.of("job", "api", "instance", "i1");
  private static final Map<String, String> HISTO_TAGS = Map.of("job", "web", "env", "prod");
  private static final Map<String, String> COUNTER_TAGS = Map.of("job", "batch", "env", "stg");

  TestResourceFactory rf;
  Node node =
      new Node(
          CasTsClientTests.class.getSimpleName(),
          "localhost:1001",
          NodeState.METRICS_CONSUMPTION_START);

  long now;
  long start;
  long step;

  GaugeGenerator gaugeGen;
  HistoGenerator histoGen;
  DeltaSumGenerator counterGen;

  @BeforeEach
  public void setup() throws BadRequestException, InterruptedException, StatisticsFrozenException {
    bootstrap();
    rf = new TestResourceFactory();

    step = Duration.of(10, ChronoUnit.MINUTES).toMillis();
    now = 60_000 * (System.currentTimeMillis() / 60_000);
    start = now - Duration.of(30, ChronoUnit.MINUTES).toMillis();

    gaugeGen = new GaugeGenerator(Duration.ofSeconds(1), 10);
    var buckets = Arrays.asList(10f, 50f, 100f);
    histoGen = new HistoGenerator(buckets, 10, Duration.ofSeconds(20));
    counterGen = new DeltaSumGenerator(10, Duration.ofMinutes(1));

    histoGen.generate(start, start + step);
    gaugeGen.populateRandom(start + step, 0.f, 10.f);
    counterGen.generate(start + 2 * step, now);

    var writer = rf.casMetricsWriter(getMapper(), node);

    var gaugeReq =
        ExportMetricsRequest.builder()
            .tenantId(TENANT)
            .metricName(GAUGE_METRIC)
            .type(MetricType.GAUGE)
            .tags(GAUGE_TAGS)
            .gauge(
                Gauge.builder()
                    .ts(Longs.toArray(gaugeGen.getTimestamps()))
                    .value(Floats.toArray(gaugeGen.getValues()))
                    .build())
            .build();

    var histoReq =
        ExportMetricsRequest.builder()
            .tenantId(TENANT)
            .metricName(HISTO_METRIC)
            .type(MetricType.HISTO)
            .tags(HISTO_TAGS)
            .histo(
                Histo.builder()
                    .histoPoints(
                        histoGen.getReadings().stream()
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

    var sumReq =
        ExportMetricsRequest.builder()
            .tenantId(TENANT)
            .metricName(COUNTER_METRIC)
            .type(MetricType.COUNTER)
            .tags(COUNTER_TAGS)
            .sum(
                Sum.builder()
                    .sumType(SumType.DELTA)
                    .sumPoints(
                        counterGen.getReadings().stream()
                            .map(r -> new SumPoint(r.start(), r.end(), r.count()))
                            .toList())
                    .build())
            .build();

    writer.onRequestArrive(gaugeReq);
    log.info(
        "Wrote for tenant: {} metric path: {}",
        gaugeReq.getTenantId(),
        MetricPaths.localPath(gaugeReq.getMetricName(), gaugeReq.getTags()));
    writer.onRequestArrive(histoReq);
    writer.onRequestArrive(sumReq);

    // wait for data visibility: probe TsReader for each metric
    var reader = rf.casTsReaders(getMapper(), node);
    var gaugeSeries = MetricPaths.univPath(TENANT, GAUGE_METRIC, GAUGE_TAGS);
    var histoSeries = MetricPaths.univPath(TENANT, HISTO_METRIC, HISTO_TAGS);
    var sumSeries = MetricPaths.univPath(TENANT, COUNTER_METRIC, COUNTER_TAGS);

    log.info("Tenant: {}", TENANT);
    log.info("Querying: {}, secondly start: {}, end: {}", gaugeSeries, start / 1000, now / 1000);
    okapiWait().until(() -> !reader.scanGauageSecondlyRes(gaugeSeries, start, now).isEmpty());
    okapiWait().until(() -> !reader.scanHisto(histoSeries, start, now).getUbs().isEmpty());
    okapiWait()
        .until(
            () ->
                !reader
                    .scanSum(
                        sumSeries, start, now, Duration.ofMinutes(1).toMillis(), SUM_TYPE.DELTA)
                    .getTs()
                    .isEmpty());
  }

  @Test
  public void clientReturnsGaugeScan() {
    var reader = rf.casTsReaders(getMapper(), node);
    MetricsMapper mapper = getMapper();
    TypeHintsDao typeHints = mapper.typeHintsDao(TestResourceFactory.KEYSPACE);

    var client = new CasTsClient();
    client.tenantId = TENANT;
    client.reader = reader;
    client.typeHintsDao = typeHints;

    long st = start + step; // where gauge exists
    long en = start + 2 * step;
    var scan = client.get(GAUGE_METRIC, GAUGE_TAGS, RESOLUTION.SECONDLY, st, en);
    var gs = (GaugeScan) scan;

    var expected = gaugeGen.avgReduction(RES_TYPE.SECONDLY);
    assertEquals(expected.getTimestamp(), gs.getTimestamps());
    assertEquals(expected.getValues(), gs.getValues());
  }

  @Test
  public void clientReturnsHistoScan() {
    var reader = rf.casTsReaders(getMapper(), node);
    MetricsMapper mapper = getMapper();
    TypeHintsDao typeHints = mapper.typeHintsDao(TestResourceFactory.KEYSPACE);

    var client = new CasTsClient();
    client.tenantId = TENANT;
    client.reader = reader;
    client.typeHintsDao = typeHints;

    long st = start; // histo exists in [start, start+step]
    long en = start + step;
    var scan = client.get(HISTO_METRIC, HISTO_TAGS, RESOLUTION.SECONDLY, st, en);
    var hs = (HistoScan) scan;

    var agg = histoGen.aggregate(st, en);
    assertEquals(agg.counts(), hs.getCounts());
    assertEquals(Arrays.asList(10f, 50f, 100f), hs.getUbs());
  }

  @Test
  public void clientReturnsSumScan() {
    var reader = rf.casTsReaders(getMapper(), node);
    MetricsMapper mapper = getMapper();
    TypeHintsDao typeHints = mapper.typeHintsDao(TestResourceFactory.KEYSPACE);

    var client = new CasTsClient();
    client.tenantId = TENANT;
    client.reader = reader;
    client.typeHintsDao = typeHints;

    long st = start + 2 * step; // counter exists in [start+2*step, now]
    long en = now;
    var scan = client.get(COUNTER_METRIC, COUNTER_TAGS, RESOLUTION.SECONDLY, st, en);
    var ss = (SumScan) scan;

    // All readings are within [st, en] and have small window; client uses large window -> all
    // included
    assertEquals(counterGen.getReadings().size(), ss.getTs().size());
  }

  @Test
  public void clientReturnsScan_whenDifferentTypesPresent() {
    var reader = rf.casTsReaders(getMapper(), node);
    MetricsMapper mapper = getMapper();
    TypeHintsDao typeHints = mapper.typeHintsDao(TestResourceFactory.KEYSPACE);

    var client = new CasTsClient();
    client.tenantId = TENANT;
    client.reader = reader;
    client.typeHintsDao = typeHints;

    long st = start; // counter exists in [start+2*step, now]
    long en = now;
    var scan = client.get(COUNTER_METRIC, COUNTER_TAGS, RESOLUTION.SECONDLY, st, en);
    var ss = (SumScan) scan;

    // All readings are within [st, en] and have small window; client uses large window -> all
    // included
    assertEquals(counterGen.getReadings().size(), ss.getTs().size());
  }
}
