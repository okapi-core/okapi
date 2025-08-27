package org.okapi.metrics.web;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.metrics.GlobalTestConfig.okapiWait;

import com.okapi.rest.metrics.MetricsPathSpecifier;
import com.okapi.rest.metrics.SearchMetricsRequestInternal;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.collections.OkapiLists;
import org.okapi.exceptions.BadRequestException;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.rocksdb.RocksDBException;

public class QueryProcessorTest {

  public static final Integer SHARD_0 = 0;
  public static final Integer SHARD_1 = 1;
  TestResourceFactory resourceFactory;
  Node node = new Node("test-node", "localhost:123", NodeState.METRICS_CONSUMPTION_START);

  @BeforeEach
  public void setup() {
    resourceFactory = new TestResourceFactory();
    resourceFactory.setUseRealClock(true);
  }

  @Test
  public void testMetricsSearch_single_shard()
      throws OutsideWindowException,
          BadRequestException,
          StatisticsFrozenException,
          InterruptedException, RocksDBException, IOException {
    // setup
    var shardMap = resourceFactory.shardMap(node);
    var readingGenerator = new ReadingGenerator(Duration.of(200, ChronoUnit.MILLIS), 120);
    var reading = readingGenerator.populateRandom(0.f, 100.f);
    shardMap.apply(SHARD_1,
            new MetricsContext("test"),
            "tenantId:series-A{label_A=value_A}",
            OkapiLists.toLongArray(reading.getTimestamps()),
            OkapiLists.toFloatArray(reading.getValues()));
    var queryProcessor = resourceFactory.queryProcessor(node);

    // check that message box gets filled
    await().atMost(Duration.of(1, ChronoUnit.SECONDS)).until(() -> {
      return !resourceFactory.messageBox(node).isEmpty();
    });

    // start writing
    resourceFactory.rocksDbStatsWriter(node).startWriting(
            resourceFactory.scheduledExecutorService(node),
            resourceFactory.rocksStore(node),
            resourceFactory.writeBackSettings(node)
            );

    await().atMost(Duration.of(20, ChronoUnit.SECONDS)).until(() -> {
      return resourceFactory.messageBox(node).isEmpty();
    });

    // query that matches
    var request =
        SearchMetricsRequestInternal.builder()
            .tenantId("tenantId")
            .startTime(System.currentTimeMillis())
            .endTime(System.currentTimeMillis() + Duration.of(120, ChronoUnit.MINUTES).toMillis())
            .pattern("series-A")
            .build();
    var results = queryProcessor.searchMetricsResponse(request);
    assertTrue(results.getResults().size() > 0);
    var result = results.getResults().get(0);
    assertEquals(result.getName(), "series-A");
    assertEquals(1, results.getResults().size());
    assertEquals(result.getTags(), Map.of("label_A", "value_A"));

    // query that doesn't match: wrong metric path
    var doesNotMatchRequest =
        SearchMetricsRequestInternal.builder()
            .tenantId("tenantId")
            .startTime(System.currentTimeMillis())
            .endTime(System.currentTimeMillis() + Duration.of(120, ChronoUnit.MINUTES).toMillis())
            .pattern("series-B")
            .build();
    var noMatchResults = queryProcessor.searchMetricsResponse(doesNotMatchRequest);
    assertEquals(0, noMatchResults.getResults().size());

    // query that doesn't match: wrong tenantid
    doesNotMatchRequest =
        SearchMetricsRequestInternal.builder()
            .tenantId("tenantId_unknown")
            .startTime(System.currentTimeMillis())
            .endTime(System.currentTimeMillis() + Duration.of(120, ChronoUnit.MINUTES).toMillis())
            .pattern("series-A")
            .build();
    noMatchResults = queryProcessor.searchMetricsResponse(doesNotMatchRequest);
    assertEquals(0, noMatchResults.getResults().size());
  }

  @Test
  public void tesMetricsSearch_multi_shard()
      throws OutsideWindowException,
          BadRequestException,
          StatisticsFrozenException,
          InterruptedException, RocksDBException, IOException {
    // setup
    var shardMap = resourceFactory.shardMap(node);
    var readingGenerator = new ReadingGenerator(Duration.of(200, ChronoUnit.MILLIS), 120);
    var reading = readingGenerator.populateRandom(0.f, 100.f);
    shardMap
        .apply(SHARD_0,
            new MetricsContext("test"),
            "tenantId:series-A{label_A=value_A}",
            OkapiLists.toLongArray(reading.getTimestamps()),
            OkapiLists.toFloatArray(reading.getValues()));
    shardMap
        .apply(SHARD_1,
            new MetricsContext("test"),
            "tenantId:series-B{label_A=value_A}",
            OkapiLists.toLongArray(reading.getTimestamps()),
            OkapiLists.toFloatArray(reading.getValues()));

    // check that message box gets filled
    okapiWait().until(() -> {
      return !resourceFactory.messageBox(node).isEmpty();
    });

    // start writing
    resourceFactory.rocksDbStatsWriter(node).startWriting(
            resourceFactory.scheduledExecutorService(node),
            resourceFactory.rocksStore(node),
            resourceFactory.writeBackSettings(node)
    );

    okapiWait().until(() -> {
      return resourceFactory.messageBox(node).isEmpty();
    });

    var queryProcessor = resourceFactory.queryProcessor(node);
    var request =
        SearchMetricsRequestInternal.builder()
            .tenantId("tenantId")
            .startTime(System.currentTimeMillis())
            .endTime(System.currentTimeMillis() + Duration.of(120, ChronoUnit.MINUTES).toMillis())
            .pattern("series-*{label_A=value_A}")
            .build();
    var results = queryProcessor.searchMetricsResponse(request);
    assertTrue(results.getResults().size() > 0);
    var metricNames = results.getResults().stream().map(MetricsPathSpecifier::getName).toList();
    assertEquals(2, results.getResults().size());
    assertTrue(metricNames.contains("series-A"));
    assertTrue(metricNames.contains("series-B"));
    var tags = results.getResults().stream().map(MetricsPathSpecifier::getTags).toList();
    assertTrue(tags.contains(Map.of("label_A", "value_A")));

    // outside range
    request =
        SearchMetricsRequestInternal.builder()
            .tenantId("tenantId")
            .startTime(System.currentTimeMillis() + Duration.of(180, ChronoUnit.MINUTES).toMillis())
            .endTime(System.currentTimeMillis() + Duration.of(240, ChronoUnit.MINUTES).toMillis())
            .pattern("series-*{label_A=value_A}")
            .build();
    results = queryProcessor.searchMetricsResponse(request);
    assertTrue(results.getResults().isEmpty());
  }
}
