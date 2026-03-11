package org.okapi.metrics.ch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clickhouse.client.api.Client;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.metrics.v1.Gauge;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.proto.resource.v1.Resource;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.ch.CreateChTablesSpec;
import org.okapi.rest.search.LabelValueFilter;
import org.okapi.rest.search.LabelValuePatternFilter;
import org.okapi.rest.search.MetricPath;
import org.okapi.rest.search.SearchMetricsRequest;
import org.okapi.testmodules.guice.TestChMetricsModule;
import org.okapi.traces.testutil.OtelShortHands;

public class ChSearchMetricsProcessorTest {

  @TempDir Path tempDir;

  private Injector injector;
  private ChSearchMetricsProcessor searchProcessor;

  // Corpus: 5 distinct metric paths
  // P1: cpu.usage  {env=prod, region=us-east, host=web-01}
  // P2: cpu.usage  {env=prod, region=eu-west, host=web-02}
  // P3: cpu.usage  {env=dev,  region=us-east, host=db-01}
  // P4: cpu.idle   {env=prod, region=us-east, host=web-01}
  // P5: memory.usage {env=prod, region=us-east, host=web-01}

  private static final Map<String, String> TAGS_PROD_WEB_01 =
      Map.of("env", "prod", "region", "us-east", "host", "web-01");
  private static final Map<String, String> TAGS_PROD_WEB_02 =
      Map.of("env", "prod", "region", "eu-west", "host", "web-02");
  private static final Map<String, String> TAGS_DEV_DB_01 =
      Map.of("env", "dev", "region", "us-east", "host", "db-01");

  @BeforeEach
  void setup() throws Exception {
    injector = Guice.createInjector(new TestChMetricsModule(tempDir.resolve("wal"), 16));
    var client = injector.getInstance(Client.class);
    searchProcessor = injector.getInstance(ChSearchMetricsProcessor.class);

    CreateChTablesSpec.migrate(client);
    client.queryAll("TRUNCATE TABLE IF EXISTS " + ChConstants.TBL_METRIC_EVENTS_META);

    var ingester = injector.getInstance(ChMetricsIngester.class);
    var driver = injector.getInstance(ChMetricsWalConsumerDriver.class);

    ingester.ingestOtelProtobuf(gauge("cpu.usage", TAGS_PROD_WEB_01));
    ingester.ingestOtelProtobuf(gauge("cpu.usage", TAGS_PROD_WEB_02));
    ingester.ingestOtelProtobuf(gauge("cpu.usage", TAGS_DEV_DB_01));
    ingester.ingestOtelProtobuf(gauge("cpu.idle", TAGS_PROD_WEB_01));
    ingester.ingestOtelProtobuf(gauge("memory.usage", TAGS_PROD_WEB_01));

    driver.onTick();
  }

  @Test
  void searchByExactMetricName() {
    var resp = search(req().metricName("cpu.usage"));
    assertEquals(3, resp.size());
    assertTrue(resp.contains(path("cpu.usage", TAGS_PROD_WEB_01)));
    assertTrue(resp.contains(path("cpu.usage", TAGS_PROD_WEB_02)));
    assertTrue(resp.contains(path("cpu.usage", TAGS_DEV_DB_01)));
  }

  @Test
  void searchByMetricPattern() {
    var resp = search(req().metricNamePattern("cpu\\..*"));
    assertEquals(4, resp.size());
    assertTrue(resp.contains(path("cpu.usage", TAGS_PROD_WEB_01)));
    assertTrue(resp.contains(path("cpu.usage", TAGS_PROD_WEB_02)));
    assertTrue(resp.contains(path("cpu.usage", TAGS_DEV_DB_01)));
    assertTrue(resp.contains(path("cpu.idle", TAGS_PROD_WEB_01)));
  }

  @Test
  void searchByMetricPatternNoMatch() {
    var resp = search(req().metricNamePattern("disk\\..*"));
    assertEquals(0, resp.size());
  }

  @Test
  void searchByLabelValue() {
    var resp = search(req().valueFilters(List.of(labelFilter("env", "prod"))));
    assertEquals(4, resp.size());
    assertTrue(resp.contains(path("cpu.usage", TAGS_PROD_WEB_01)));
    assertTrue(resp.contains(path("cpu.usage", TAGS_PROD_WEB_02)));
    assertTrue(resp.contains(path("cpu.idle", TAGS_PROD_WEB_01)));
    assertTrue(resp.contains(path("memory.usage", TAGS_PROD_WEB_01)));
  }

  @Test
  void searchByLabelValueNoMatch() {
    var resp = search(req().valueFilters(List.of(labelFilter("env", "staging"))));
    assertEquals(0, resp.size());
  }

  @Test
  void searchByLabelValuePattern() {
    var resp = search(req().patternFilters(List.of(patternFilter("host", "web-.*"))));
    assertEquals(4, resp.size());
    assertTrue(resp.contains(path("cpu.usage", TAGS_PROD_WEB_01)));
    assertTrue(resp.contains(path("cpu.usage", TAGS_PROD_WEB_02)));
    assertTrue(resp.contains(path("cpu.idle", TAGS_PROD_WEB_01)));
    assertTrue(resp.contains(path("memory.usage", TAGS_PROD_WEB_01)));
  }

  @Test
  void searchByLabelValuePatternNoMatch() {
    var resp = search(req().patternFilters(List.of(patternFilter("host", "cache-.*"))));
    assertEquals(0, resp.size());
  }

  @Test
  void searchByMetricPatternAndLabelValue() {
    var resp =
        search(
            req()
                .metricNamePattern("cpu\\..*")
                .valueFilters(List.of(labelFilter("env", "prod"))));
    assertEquals(3, resp.size());
    assertTrue(resp.contains(path("cpu.usage", TAGS_PROD_WEB_01)));
    assertTrue(resp.contains(path("cpu.usage", TAGS_PROD_WEB_02)));
    assertTrue(resp.contains(path("cpu.idle", TAGS_PROD_WEB_01)));
  }

  @Test
  void searchByMetricPatternAndLabelValuePattern() {
    var resp =
        search(
            req()
                .metricNamePattern("cpu\\..*")
                .patternFilters(List.of(patternFilter("host", "web-.*"))));
    assertEquals(3, resp.size());
    assertTrue(resp.contains(path("cpu.usage", TAGS_PROD_WEB_01)));
    assertTrue(resp.contains(path("cpu.usage", TAGS_PROD_WEB_02)));
    assertTrue(resp.contains(path("cpu.idle", TAGS_PROD_WEB_01)));
  }

  @Test
  void searchNoFilters() {
    var resp = search(req());
    assertEquals(5, resp.size());
  }

  // --- helpers ---

  // Year 2100 in millis — large enough for tests, safely within DateTime64(3) range
  private static final long FAR_FUTURE_MS = 4_102_444_800_000L;

  private Set<MetricPath> search(SearchMetricsRequest.SearchMetricsRequestBuilder builder) {
    var request = builder.tsStartMillis(0).tsEndMillis(FAR_FUTURE_MS).build();
    return searchProcessor.searchMetricsResponse(request).getMatchingPaths().stream()
        .collect(Collectors.toSet());
  }

  private SearchMetricsRequest.SearchMetricsRequestBuilder req() {
    return SearchMetricsRequest.builder();
  }

  private MetricPath path(String metric, Map<String, String> labels) {
    return MetricPath.builder().metric(metric).labels(labels).build();
  }

  private LabelValueFilter labelFilter(String label, String value) {
    return LabelValueFilter.builder().label(label).value(value).build();
  }

  private LabelValuePatternFilter patternFilter(String label, String pattern) {
    return LabelValuePatternFilter.builder().label(label).pattern(pattern).build();
  }

  private ExportMetricsServiceRequest gauge(String metricName, Map<String, String> tags) {
    var point =
        NumberDataPoint.newBuilder()
            .setTimeUnixNano(1_000L * 1_000_000)
            .setAsDouble(1.0)
            .addAllAttributes(OtelShortHands.keyValues(tags))
            .build();
    var metric =
        Metric.newBuilder()
            .setName(metricName)
            .setGauge(Gauge.newBuilder().addDataPoints(point).build())
            .build();
    var resourceMetrics =
        ResourceMetrics.newBuilder()
            .setResource(Resource.newBuilder().build())
            .addScopeMetrics(ScopeMetrics.newBuilder().addMetrics(metric).build())
            .build();
    return ExportMetricsServiceRequest.newBuilder().addResourceMetrics(resourceMetrics).build();
  }
}
