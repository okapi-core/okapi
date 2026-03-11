/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.ch;

import com.clickhouse.client.api.Client;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.metrics.v1.*;
import io.opentelemetry.proto.resource.v1.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.ch.CreateChTablesSpec;
import org.okapi.chtest.ChTestOnlyUtils;
import org.okapi.rest.metrics.query.GetMetricsRequest;
import org.okapi.rest.metrics.query.GetSumsQueryConfig;
import org.okapi.rest.metrics.query.METRIC_TYPE;
import org.okapi.testmodules.guice.TestChMetricsModule;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ChSumTests {
  @TempDir java.nio.file.Path tempDir;

  private Injector injector;
  private Client client;
  private String testSession;

  @BeforeEach
  void setup() {
    testSession = UUID.randomUUID().toString();
    injector = Guice.createInjector(new TestChMetricsModule(tempDir.resolve("wal"), 16));
    client = injector.getInstance(Client.class);
    CreateChTablesSpec.migrate(client);
    ChTestOnlyUtils.truncateTable(client, ChConstants.TBL_SUM);
    ChTestOnlyUtils.truncateTable(client, ChConstants.TBL_METRIC_EVENTS_META);
  }

  @Test
  void deltaAggregateSumsTwoPoints() throws Exception {
    var ingester = injector.getInstance(ChMetricsIngester.class);
    var driver = injector.getInstance(ChMetricsWalConsumerDriver.class);
    var qp = injector.getInstance(ChMetricsQueryProcessor.class);

    var resource = "svc-sum-" + UUID.randomUUID();
    var metric = "metric_sum";
    var tags = Map.of("env", "dev", "test-session", testSession);

    var req =
        buildSumRequest(
            resource,
            metric,
            tags,
            AggregationTemporality.AGGREGATION_TEMPORALITY_DELTA,
            List.of(numberPoint(1_000L, 2_000L, 3.0), numberPoint(2_000L, 3_000L, 4.0)));

    ingester.ingestOtelProtobuf(req);
    driver.onTick();

    var queryReq =
        GetMetricsRequest.builder()
            .metric(metric)
            .tags(tags)
            .start(0)
            .end(5_000)
            .metricType(METRIC_TYPE.SUM)
            .sumsQueryConfig(
                GetSumsQueryConfig.builder()
                    .temporality(GetSumsQueryConfig.TEMPORALITY.DELTA_AGGREGATE)
                    .build())
            .build();

    var resp = qp.getMetricsResponse(queryReq);
    assertNotNull(resp.getSumsResponse());
    var sums = resp.getSumsResponse().getSums();
    assertEquals(1, sums.size());
    assertEquals(7L, sums.get(0).getCount());

    var cumulativeReq =
        GetMetricsRequest.builder()
            .metric(metric)
            .tags(tags)
            .start(0)
            .end(5_000)
            .metricType(METRIC_TYPE.SUM)
            .sumsQueryConfig(
                GetSumsQueryConfig.builder()
                    .temporality(GetSumsQueryConfig.TEMPORALITY.CUMULATIVE)
                    .build())
            .build();
    var cumulativeResp = qp.getMetricsResponse(cumulativeReq);
    assertNull(cumulativeResp.getSumsResponse());
  }

  @Test
  void cumulativeReturnsLatestValueAndDeltaAggregateReturnsNull() throws Exception {
    var ingester = injector.getInstance(ChMetricsIngester.class);
    var driver = injector.getInstance(ChMetricsWalConsumerDriver.class);
    var qp = injector.getInstance(ChMetricsQueryProcessor.class);

    var resource = "svc-sum-" + UUID.randomUUID();
    var metric = "metric_sum_cumulative";
    var tags = Map.of("env", "dev", "test-session", testSession);

    var req =
        buildSumRequest(
            resource,
            metric,
            tags,
            AggregationTemporality.AGGREGATION_TEMPORALITY_CUMULATIVE,
            List.of(
                numberPoint(1_000L, 2_000L, 10.0),
                numberPoint(2_000L, 3_000L, 15.0),
                numberPoint(3_000L, 4_000L, 19.0)));

    ingester.ingestOtelProtobuf(req);
    driver.onTick();

    var cumulativeReq =
        GetMetricsRequest.builder()
            .metric(metric)
            .tags(tags)
            .start(0)
            .end(5_000)
            .metricType(METRIC_TYPE.SUM)
            .sumsQueryConfig(
                GetSumsQueryConfig.builder()
                    .temporality(GetSumsQueryConfig.TEMPORALITY.CUMULATIVE)
                    .build())
            .build();
    var cumulativeResp = qp.getMetricsResponse(cumulativeReq);
    assertNotNull(cumulativeResp.getSumsResponse());
    var sums = cumulativeResp.getSumsResponse().getSums();
    assertEquals(1, sums.size());
    assertEquals(19L, sums.get(0).getCount());

    var deltaReq =
        GetMetricsRequest.builder()
            .metric(metric)
            .tags(tags)
            .start(0)
            .end(5_000)
            .metricType(METRIC_TYPE.SUM)
            .sumsQueryConfig(
                GetSumsQueryConfig.builder()
                    .temporality(GetSumsQueryConfig.TEMPORALITY.DELTA_AGGREGATE)
                    .build())
            .build();
    var deltaResp = qp.getMetricsResponse(deltaReq);
    assertNull(deltaResp.getSumsResponse());
  }

  private ExportMetricsServiceRequest buildSumRequest(
      String resourceName,
      String metricName,
      Map<String, String> tags,
      AggregationTemporality temporality,
      List<NumberDataPoint> points) {
    var sum =
        Sum.newBuilder()
            .setAggregationTemporality(temporality)
            .setIsMonotonic(false)
            .addAllDataPoints(points)
            .build();
    var metric = Metric.newBuilder().setName(metricName).setSum(sum).build();
    var scopeMetrics = ScopeMetrics.newBuilder().addMetrics(metric).build();
    var resource =
        Resource.newBuilder()
            .addAttributes(
                io.opentelemetry.proto.common.v1.KeyValue.newBuilder()
                    .setKey("service.name")
                    .setValue(
                        io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                            .setStringValue(resourceName)
                            .build())
                    .build())
            .build();
    var resourceMetrics =
        ResourceMetrics.newBuilder().setResource(resource).addScopeMetrics(scopeMetrics).build();
    return ExportMetricsServiceRequest.newBuilder().addResourceMetrics(resourceMetrics).build();
  }

  private NumberDataPoint numberPoint(long startMs, long endMs, double val) {
    var builder =
        NumberDataPoint.newBuilder()
            .setStartTimeUnixNano(startMs * 1_000_000)
            .setTimeUnixNano(endMs * 1_000_000)
            .setAsDouble(val);
    builder.addAttributes(
        io.opentelemetry.proto.common.v1.KeyValue.newBuilder()
            .setKey("env")
            .setValue(
                io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                    .setStringValue("dev")
                    .build())
            .build());
    builder.addAttributes(
        io.opentelemetry.proto.common.v1.KeyValue.newBuilder()
            .setKey("test-session")
            .setValue(
                io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                    .setStringValue(testSession)
                    .build())
            .build());
    return builder.build();
  }
}
