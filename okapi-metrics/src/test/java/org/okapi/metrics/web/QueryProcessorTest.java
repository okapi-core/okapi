package org.okapi.metrics.web;

import static org.junit.jupiter.api.Assertions.*;

import org.okapi.collections.OkapiLists;
import org.okapi.exceptions.BadRequestException;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.TestResourceFactory;
import com.okapi.rest.metrics.MetricsPathSpecifier;
import com.okapi.rest.metrics.SearchMetricsRequestInternal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QueryProcessorTest {

  TestResourceFactory resourceFactory;
  Node node = new Node("test-node", "localhost:123", NodeState.METRICS_CONSUMPTION_START);

  @BeforeEach
  public void setup() {
    resourceFactory = new TestResourceFactory();
  }

  @Test
  public void testMetricsSearch_single_shard() throws OutsideWindowException, BadRequestException {
    // setup
    var shardMap = resourceFactory.shardMap(node);
    var readingGenerator = new ReadingGenerator(Duration.of(200, ChronoUnit.MILLIS), 120);
    var reading = readingGenerator.populateRandom(0.f, 100.f);
    shardMap
        .get(0)
        .writeBatch(
            new MetricsContext("test"),
            "tenantId:series-A{label_A=value_A}",
            OkapiLists.toLongArray(reading.getTimestamps()),
            OkapiLists.toFloatArray(reading.getValues()));
    var queryProcessor = resourceFactory.queryProcessor(node);

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
  public void tesMetricsSearch_multi_shard() throws OutsideWindowException, BadRequestException {
    // setup
    var shardMap = resourceFactory.shardMap(node);
    var readingGenerator = new ReadingGenerator(Duration.of(200, ChronoUnit.MILLIS), 120);
    var reading = readingGenerator.populateRandom(0.f, 100.f);
    shardMap
        .get(0)
        .writeBatch(
            new MetricsContext("test"),
            "tenantId:series-A{label_A=value_A}",
            OkapiLists.toLongArray(reading.getTimestamps()),
            OkapiLists.toFloatArray(reading.getValues()));
    shardMap
        .get(1)
        .writeBatch(
            new MetricsContext("test"),
            "tenantId:series-B{label_A=value_A}",
            OkapiLists.toLongArray(reading.getTimestamps()),
            OkapiLists.toFloatArray(reading.getValues()));
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
    // todo: make this thing only have 1 result not O(hash-keys)
    var tags = results.getResults().stream().map(MetricsPathSpecifier::getTags).toList();
    assertTrue(tags.contains(Map.of("label_A", "value_A")));

    // outside range
    request =
        SearchMetricsRequestInternal.builder()
            .tenantId("tenantId")
            .startTime(System.currentTimeMillis() + Duration.of(140, ChronoUnit.MINUTES).toMillis())
            .endTime(System.currentTimeMillis() + Duration.of(160, ChronoUnit.MINUTES).toMillis())
            .pattern("series-*{label_A=value_A}")
            .build();
    results = queryProcessor.searchMetricsResponse(request);
    assertTrue(results.getResults().isEmpty());
  }
}
