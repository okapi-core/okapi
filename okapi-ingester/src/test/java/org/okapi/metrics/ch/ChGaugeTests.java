/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.ch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.clickhouse.client.api.Client;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.ch.CreateChTablesSpec;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.rest.metrics.query.GaugeQueryConfig;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.METRIC_TYPE;
import org.okapi.testmodules.guice.TestChMetricsModule;

public class ChGaugeTests {

  @TempDir Path tempDir;

  private Injector injector;
  private Client client;
  private final String testSession = java.util.UUID.randomUUID().toString();
  private final MetricsTestingOtelFactory otelFactory = new MetricsTestingOtelFactory(testSession);

  @BeforeEach
  void setup() {
    injector = Guice.createInjector(new TestChMetricsModule(tempDir.resolve("wal"), 16));
    client = injector.getInstance(Client.class);
    CreateChTablesSpec.migrate(client);
    truncateGaugeTable();
  }

  @Test
  void singleDatapointGaugeQuery() throws Exception {
    var ingester = injector.getInstance(ChMetricsIngester.class);
    var driver = injector.getInstance(ChMetricsWalConsumerDriver.class);
    var qp = injector.getInstance(ChMetricsQueryProcessor.class);

    ingester.ingestOtelProtobuf(
        otelFactory.buildGaugeRequest("svc-1", "metric_1", List.of(1_000L), List.of(1.0)));
    driver.onTick();

    var req =
        GetMetricsRequest.builder()
            .svc("svc-1")
            .metric("metric_1")
            .tags(Map.of("env", "dev"))
            .tags(Map.of("env", "dev", "test-session", testSession))
            .start(0)
            .end(10_000)
            .metricType(METRIC_TYPE.GAUGE)
            .gaugeQueryConfig(new GaugeQueryConfig(RES_TYPE.SECONDLY, AGG_TYPE.AVG))
            .build();

    var resp = qp.getMetricsResponse(req).getGaugeResponse();
    assertNotNull(resp);
    assertEquals(List.of(1_000L), resp.getTimes());
    assertEquals(List.of(1.0f), resp.getValues());
  }

  @Test
  void twoDatapointsGaugeQuery() throws Exception {
    var ingester = injector.getInstance(ChMetricsIngester.class);
    var driver = injector.getInstance(ChMetricsWalConsumerDriver.class);
    var qp = injector.getInstance(ChMetricsQueryProcessor.class);

    ingester.ingestOtelProtobuf(
        otelFactory.buildGaugeRequest(
            "svc-2", "metric_2", List.of(1_000L, 2_000L), List.of(1.0, 2.0)));
    driver.onTick();

    var req =
        GetMetricsRequest.builder()
            .svc("svc-2")
            .metric("metric_2")
            .tags(Map.of("env", "dev", "test-session", testSession))
            .start(0)
            .end(10_000)
            .metricType(METRIC_TYPE.GAUGE)
            .gaugeQueryConfig(new GaugeQueryConfig(RES_TYPE.SECONDLY, AGG_TYPE.AVG))
            .build();

    var resp = qp.getMetricsResponse(req).getGaugeResponse();
    assertNotNull(resp);
    assertEquals(List.of(1_000L, 2_000L), resp.getTimes());
    assertEquals(List.of(1.0f, 2.0f), resp.getValues());
  }

  @Test
  void tagMismatchReturnsEmpty() throws Exception {
    var ingester = injector.getInstance(ChMetricsIngester.class);
    var driver = injector.getInstance(ChMetricsWalConsumerDriver.class);
    var qp = injector.getInstance(ChMetricsQueryProcessor.class);

    ingester.ingestOtelProtobuf(
        otelFactory.buildGaugeRequest("svc-3", "metric_3", List.of(1_000L), List.of(1.0)));
    driver.onTick();

    var req =
        GetMetricsRequest.builder()
            .svc("svc-3")
            .metric("metric_3")
            .tags(Map.of("env", "prod", "test-session", testSession))
            .start(0)
            .end(10_000)
            .metricType(METRIC_TYPE.GAUGE)
            .gaugeQueryConfig(new GaugeQueryConfig(RES_TYPE.SECONDLY, AGG_TYPE.AVG))
            .build();

    var resp = qp.getMetricsResponse(req).getGaugeResponse();
    assertNotNull(resp);
    assertEquals(List.of(), resp.getTimes());
    assertEquals(List.of(), resp.getValues());
  }

  @Test
  void timeWindowOutsideRangeReturnsEmpty() throws Exception {
    var ingester = injector.getInstance(ChMetricsIngester.class);
    var driver = injector.getInstance(ChMetricsWalConsumerDriver.class);
    var qp = injector.getInstance(ChMetricsQueryProcessor.class);

    ingester.ingestOtelProtobuf(
        otelFactory.buildGaugeRequest("svc-4", "metric_4", List.of(1_000L), List.of(1.0)));
    driver.onTick();

    var req =
        GetMetricsRequest.builder()
            .svc("svc-4")
            .metric("metric_4")
            .tags(Map.of("env", "dev", "test-session", testSession))
            .start(5_000)
            .end(10_000)
            .metricType(METRIC_TYPE.GAUGE)
            .gaugeQueryConfig(new GaugeQueryConfig(RES_TYPE.SECONDLY, AGG_TYPE.AVG))
            .build();

    var resp = qp.getMetricsResponse(req).getGaugeResponse();
    assertNotNull(resp);
    assertEquals(List.of(), resp.getTimes());
    assertEquals(List.of(), resp.getValues());
  }

  @Test
  void multipleResourcesFilterBySvc() throws Exception {
    var ingester = injector.getInstance(ChMetricsIngester.class);
    var driver = injector.getInstance(ChMetricsWalConsumerDriver.class);
    var qp = injector.getInstance(ChMetricsQueryProcessor.class);

    ingester.ingestOtelProtobuf(
        otelFactory.buildGaugeRequest("svc-keep", "metric_multi", List.of(1_000L), List.of(1.0)));
    ingester.ingestOtelProtobuf(
        otelFactory.buildGaugeRequest("svc-ignore", "metric_multi", List.of(1_000L), List.of(5.0)));
    driver.onTick();

    var req =
        GetMetricsRequest.builder()
            .svc("svc-keep")
            .metric("metric_multi")
            .tags(Map.of("env", "dev", "test-session", testSession))
            .start(0)
            .end(10_000)
            .metricType(METRIC_TYPE.GAUGE)
            .gaugeQueryConfig(new GaugeQueryConfig(RES_TYPE.SECONDLY, AGG_TYPE.AVG))
            .build();

    var resp = qp.getMetricsResponse(req).getGaugeResponse();
    assertNotNull(resp);
    assertEquals(List.of(1_000L), resp.getTimes());
    assertEquals(List.of(1.0f), resp.getValues());
  }

  @Test
  void sameBucketAggregationAvg() throws Exception {
    var ingester = injector.getInstance(ChMetricsIngester.class);
    var driver = injector.getInstance(ChMetricsWalConsumerDriver.class);
    var qp = injector.getInstance(ChMetricsQueryProcessor.class);

    ingester.ingestOtelProtobuf(
        otelFactory.buildGaugeRequest(
            "svc-5", "metric_same_bucket", List.of(1_000L, 1_050L), List.of(1.0, 3.0)));
    driver.onTick();

    var req =
        GetMetricsRequest.builder()
            .svc("svc-5")
            .metric("metric_same_bucket")
            .tags(Map.of("env", "dev", "test-session", testSession))
            .start(0)
            .end(10_000)
            .metricType(METRIC_TYPE.GAUGE)
            .gaugeQueryConfig(new GaugeQueryConfig(RES_TYPE.SECONDLY, AGG_TYPE.AVG))
            .build();

    var resp = qp.getMetricsResponse(req).getGaugeResponse();
    assertNotNull(resp);
    assertEquals(List.of(1_000L), resp.getTimes());
    assertEquals(List.of(2.0f), resp.getValues());
  }

  @Test
  void minutelyResolutionSum() throws Exception {
    var ingester = injector.getInstance(ChMetricsIngester.class);
    var driver = injector.getInstance(ChMetricsWalConsumerDriver.class);
    var qp = injector.getInstance(ChMetricsQueryProcessor.class);

    ingester.ingestOtelProtobuf(
        otelFactory.buildGaugeRequest(
            "svc-6", "metric_minutely", List.of(1_000L, 30_000L), List.of(2.0, 5.0)));
    driver.onTick();

    var req =
        GetMetricsRequest.builder()
            .svc("svc-6")
            .metric("metric_minutely")
            .tags(Map.of("env", "dev", "test-session", testSession))
            .start(0)
            .end(120_000)
            .metricType(METRIC_TYPE.GAUGE)
            .gaugeQueryConfig(new GaugeQueryConfig(RES_TYPE.MINUTELY, AGG_TYPE.SUM))
            .build();

    var resp = qp.getMetricsResponse(req).getGaugeResponse();
    assertNotNull(resp);
    assertEquals(1, resp.getTimes().size());
    assertEquals(1, resp.getValues().size());
    assertEquals(7.0f, resp.getValues().getFirst());
  }

  private void truncateGaugeTable() {
    client.queryAll("TRUNCATE TABLE IF EXISTS okapi_metrics.gauge_raw_samples");
  }
}
