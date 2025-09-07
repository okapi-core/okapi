package org.okapi.metrics.fdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.collections.OkapiLists;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.singletons.FdbSingletonFactory;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.rest.metrics.SubmitMetricsRequestInternal;

public class FdbTsSearcherTests {

  public static final String TENANT = "tenant-id";

  FdbSingletonFactory fdbSingletonFactory;
  Node node = new Node("test-node", "localhost:123", NodeState.METRICS_CONSUMPTION_START);
  String METRIC_PREFIX = "metrics_" + System.currentTimeMillis();
  long start = -1;
  long end = -1;

  @BeforeEach
  void setup() throws InterruptedException, StatisticsFrozenException {
    fdbSingletonFactory = new FdbSingletonFactory();
    var box = fdbSingletonFactory.messageBox(node);
    var generator = new ReadingGenerator(Duration.of(30, ChronoUnit.SECONDS), 5); // 10 datapoints
    var reading = generator.populateRandom(0.f, 1.f);
    var request1 =
        SubmitMetricsRequestInternal.builder()
            .tenantId(TENANT)
            .metricName(METRIC_PREFIX + "_" + "dummy")
            .tags(Map.of("key1", "value1"))
            .ts(OkapiLists.toLongArray(reading.getTimestamps()))
            .values(OkapiLists.toFloatArray(reading.getValues()))
            .build();
    var request2 =
        SubmitMetricsRequestInternal.builder()
            .tenantId(TENANT)
            .metricName(METRIC_PREFIX + "_" + "dummy2")
            .tags(Map.of("key1", "value1", "key2", "value2"))
            .ts(OkapiLists.toLongArray(reading.getTimestamps()))
            .values(OkapiLists.toFloatArray(reading.getValues()))
            .build();
    box.push(request1);
    box.push(request2);
    var writer = fdbSingletonFactory.fdbWriter(node);
    writer.writeOnce();
    assertTrue(box.isEmpty());
    start = reading.getTimestamps().stream().reduce(Math::min).get();
    end = reading.getTimestamps().stream().reduce(Math::max).get();
  }

  @Test
  public void testSinglePath() throws InterruptedException, StatisticsFrozenException {
    var pattern = METRIC_PREFIX + "_*";
    var searcher = fdbSingletonFactory.fdbTsSearcher(node);
    var results = searcher.search(TENANT, pattern, start, end);
    assertEquals(2, results.size());
  }

  @Test
  public void testSingleMatch() throws InterruptedException, StatisticsFrozenException {
    var pattern = METRIC_PREFIX + "_dummy";
    var searcher = fdbSingletonFactory.fdbTsSearcher(node);
    var results = searcher.search(TENANT, pattern, start, end);
    assertEquals(1, results.size());
  }

  @Test
  public void testKeyValuePatternMatch() {
    var pattern = METRIC_PREFIX + "_*{key1=*}";
    var searcher = fdbSingletonFactory.fdbTsSearcher(node);
    var results = searcher.search(TENANT, pattern, start, end);
    assertEquals(2, results.size());
  }

  @Test
  public void testKeyValueSingleMatch() {
    var pattern = METRIC_PREFIX + "_*{key2=*}";
    var searcher = fdbSingletonFactory.fdbTsSearcher(node);
    var results = searcher.search(TENANT, pattern, start, end);
    assertEquals(1, results.size());
  }

  @Test
  public void testTwoKeys() {
    var pattern = METRIC_PREFIX + "_*{key2=*, key1=*}";
    var searcher = fdbSingletonFactory.fdbTsSearcher(node);
    var results = searcher.search(TENANT, pattern, start, end);
    assertEquals(1, results.size());
  }
}
