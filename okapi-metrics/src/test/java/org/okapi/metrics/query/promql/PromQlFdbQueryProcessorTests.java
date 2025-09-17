package org.okapi.metrics.query.promql;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.okapi.exceptions.BadRequestException;
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

public class PromQlFdbQueryProcessorTests {

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
  public void testNoOpQuery() throws EvaluationException, BadRequestException {
    var clientFactory = testResourceFactory.getFdbSingletonFactory().fdbTsClientFactory(node);
    var executor = Executors.newScheduledThreadPool(1);
    var queryProcessor =
        new PromQlQueryProcessor(
            executor, testResourceFactory.statisticsMerger(), clientFactory, seriesDiscovery);
    String promql = "avg_over_time(http_requests[2m])";
    queryProcessor.queryRange(
        TENANT, promql, toDoubleSeconds(now), toDoubleSeconds(now + 10_000), toStepSeconds(1_000));
    queryProcessor.queryRange(
        TENANT, promql, toRfc3339Date(now), toRfc3339Date(now + 10_000), toStepSeconds(1_000));
  }

  @ParameterizedTest
  @MethodSource("timeTransformers")
  public void tesAvgQuery(Function<Long, String> millisToStr)
      throws StatisticsFrozenException,
          OutsideWindowException,
          IOException,
          InterruptedException,
          EvaluationException,
          BadRequestException {
    var testCase = new PromQLSmallTestCase(now, testResourceFactory, node);
    String promql = "avg_over_time(http_requests[2m])";
    var queryProcessor = testCase.queryProcessor(testResourceFactory, executor, node);

    var result =
        queryProcessor.queryRange(
            testCase.TENANT_ID,
            promql,
            millisToStr.apply(testCase.t0),
            millisToStr.apply(testCase.t3),
            toStepSeconds(60_000));
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

  @ParameterizedTest
  @MethodSource("timeTransformers")
  public void testNoResultWithoutMatchingTenant(Function<Long, String> millisToStr)
      throws StatisticsFrozenException,
          OutsideWindowException,
          IOException,
          InterruptedException,
          EvaluationException,
          BadRequestException {
    var testCase = new PromQLSmallTestCase(now, testResourceFactory, node);
    String promql = "avg_over_time(http_requests[2m])";
    var queryProcessor = testCase.queryProcessor(testResourceFactory, executor, node);

    var result =
        queryProcessor.queryRange(
            TENANT,
            promql,
            millisToStr.apply(testCase.t0),
            millisToStr.apply(testCase.t3),
            toStepSeconds(60_000));
    assertNotNull(result);
    assertEquals(ValueType.INSTANT_VECTOR, result.type());
    var iv = (InstantVectorResult) result;
    assertTrue(iv.data().isEmpty());
  }

  public static Stream<Arguments> timeTransformers() {
    Function<Long, String> toSeconds = PromQlFdbQueryProcessorTests::toDoubleSeconds;
    Function<Long, String> toDate = PromQlFdbQueryProcessorTests::toRfc3339Date;
    return Stream.of(Arguments.of(toSeconds, toDate));
  }

  public static String toDoubleSeconds(long ts) {
    return Double.toString((0. + ts) / 1000);
  }

  public static String toRfc3339Date(long ts) {
    return Instant.ofEpochMilli(ts)
        .atOffset(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }

  public static String toStepSeconds(long stepMillis) {
    return (stepMillis / 1000) + "s";
  }
}
