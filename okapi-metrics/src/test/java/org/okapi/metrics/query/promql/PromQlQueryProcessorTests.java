package org.okapi.metrics.query.promql;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.sharding.fakes.FixedShardsAndSeriesAssigner;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.promql.eval.InstantVectorResult;
import org.okapi.promql.eval.ValueType;
import org.okapi.promql.eval.VectorData;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.extractor.TimeSeriesExtractor;

public class PromQlQueryProcessorTests {

  public static final String TENANT = "tenant";
  Node node = new Node("test-node-1", "localhost:1", NodeState.METRICS_CONSUMPTION_START);
  TestResourceFactory testResourceFactory;
  FixedShardsAndSeriesAssigner shardsAndSeriesAssigner;
  SeriesDiscoveryFactory seriesDiscovery;
  ExecutorService executor = Executors.newScheduledThreadPool(1);

  Long now = System.currentTimeMillis();

  @BeforeEach
  public void setup() throws IOException {
    testResourceFactory = new TestResourceFactory();
    shardsAndSeriesAssigner = new FixedShardsAndSeriesAssigner();
    shardsAndSeriesAssigner.assignShard(0, 1, node.id());
    seriesDiscovery = testResourceFactory.pathSetSeriesDiscovery(node);
  }

  @Test
  public void testNoOpQuery() throws EvaluationException {
    var clientFactory =
        testResourceFactory.rocksMetricsClientFactory(shardsAndSeriesAssigner, node);
    var executor = Executors.newScheduledThreadPool(1);
    var queryProcessor =
        new PromQlQueryProcessor(
            executor, testResourceFactory.statisticsMerger(), clientFactory, seriesDiscovery);
    String promql = "avg_over_time(http_requests[2m])";
    queryProcessor.process(TENANT, promql, now, now + 10_000, 1_000);
  }

  @Test
  public void tesAvgQuery()
      throws StatisticsFrozenException,
          OutsideWindowException,
          IOException,
          InterruptedException,
          EvaluationException {
    var testCase = new PromQLSmallTestCase(now, testResourceFactory, node, shardsAndSeriesAssigner);
    String promql = "avg_over_time(http_requests[2m])";
    var queryProcessor =
        testCase.queryProcessor(testResourceFactory, shardsAndSeriesAssigner, executor, node);

    var result =
        queryProcessor.process(testCase.TENANT_ID, promql, testCase.t0, testCase.t3, 60_000);
    assertNotNull(result);
    assertEquals(ValueType.INSTANT_VECTOR, result.type());

    var iv = (InstantVectorResult) result;
    var seriesId =
        new VectorData.SeriesId("http_requests", new VectorData.Labels(Collections.emptyMap()));
    var values =
        Arrays.asList(
            TimeSeriesExtractor.findValue(iv, seriesId, testCase.t0),
            TimeSeriesExtractor.findValue(iv, seriesId, testCase.t1),
            TimeSeriesExtractor.findValue(iv, seriesId, testCase.t2),
            TimeSeriesExtractor.findValue(iv, seriesId, testCase.t3));
    assertEquals(Arrays.asList(10f, 15f, 25f, 35f), values);
  }

  @Test
  public void testNoResultWithoutMatchingTenant()
      throws StatisticsFrozenException,
          OutsideWindowException,
          IOException,
          InterruptedException,
          EvaluationException {
    var testCase = new PromQLSmallTestCase(now, testResourceFactory, node, shardsAndSeriesAssigner);
    String promql = "avg_over_time(http_requests[2m])";
    var queryProcessor =
        testCase.queryProcessor(testResourceFactory, shardsAndSeriesAssigner, executor, node);

    var result = queryProcessor.process(TENANT, promql, testCase.t0, testCase.t3, 60_000);
    assertNotNull(result);
    assertEquals(ValueType.INSTANT_VECTOR, result.type());
    var iv = (InstantVectorResult) result;
    assertTrue(iv.data().isEmpty());
  }
}
