package org.okapi.metrics.fdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.collections.OkapiLists;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.singletons.FdbSingletonFactory;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.rest.metrics.SubmitMetricsRequestInternal;

public class FdbWriterTests {

  Node node = new Node("test-node", "localhost", NodeState.METRICS_CONSUMPTION_START);
  public static final String METRIC = "metric_" + System.currentTimeMillis();
  public static final String METRIC_2 = "metric2_" + System.currentTimeMillis();
  public static final String TENANT_ID = "tenant_id";
  SubmitMetricsRequestInternal.SubmitMetricsRequestInternalBuilder prototype;
  SubmitMetricsRequestInternal.SubmitMetricsRequestInternalBuilder prototype2;
  public static final Map<String, String> TAGS =
      Map.of(
          "key1", "value1",
          "key2", "value2");
  FdbSingletonFactory fdbSingletonFactory;

  @BeforeEach
  void setUp() {
    fdbSingletonFactory = new FdbSingletonFactory();
    prototype =
        SubmitMetricsRequestInternal.builder().tenantId(TENANT_ID).metricName(METRIC).tags(TAGS);
    prototype2 =
        SubmitMetricsRequestInternal.builder().tenantId(TENANT_ID).metricName(METRIC_2).tags(TAGS);
  }

  @Test
  public void testWriteOnceClearsOutInbox() throws StatisticsFrozenException, InterruptedException {
    var writer = fdbSingletonFactory.fdbWriter(node);
    var messageBox = fdbSingletonFactory.messageBox(node);
    var readingGen = new ReadingGenerator(Duration.of(1, ChronoUnit.SECONDS), 1);
    var gen = readingGen.populateRandom(0.f, 100.f);
    var singleRequest =
        prototype
            .ts(OkapiLists.toLongArray(gen.getTimestamps()))
            .values(OkapiLists.toFloatArray(gen.getValues()))
            .build();

    messageBox.push(singleRequest);
    writer.writeOnce();
    assertTrue(messageBox.isEmpty());
  }

  @Test
  public void testSearchGroupBy_secondly() throws StatisticsFrozenException {
    // test with no values, multiple paths, single path -> should have the proper expected counts.
    var writer = fdbSingletonFactory.fdbWriter(node);
    var readingGen = new ReadingGenerator(Duration.of(100, ChronoUnit.MILLIS), 1);
    var gen = readingGen.populateRandom(0.f, 100.f);
    var singleRequest =
        prototype
            .ts(OkapiLists.toLongArray(gen.getTimestamps()))
            .values(OkapiLists.toFloatArray(gen.getValues()))
            .build();

    var groups = writer.groupBy(Arrays.asList(singleRequest), writer::fdbSecondlyBucket);
    var avgReduction = readingGen.avgReduction(RES_TYPE.SECONDLY);
    assertEquals(avgReduction.getTimestamp().size(), groups.size());
    var map = avgReduction.asMap();
    var groupsAsMap =
        groups.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().bucket() * 1000, e -> e.getValue().avg()));
    assertEquals(groupsAsMap, map);
  }

  @Test
  public void testSearchGroupBy_minutely() throws StatisticsFrozenException {
    // test with no values, multiple paths, single path -> should have the proper expected counts.
    var writer = fdbSingletonFactory.fdbWriter(node);
    var readingGen = new ReadingGenerator(Duration.of(60, ChronoUnit.MILLIS), 5);
    var gen = readingGen.populateRandom(0.f, 100.f);
    var singleRequest =
        prototype
            .ts(OkapiLists.toLongArray(gen.getTimestamps()))
            .values(OkapiLists.toFloatArray(gen.getValues()))
            .build();

    var groups = writer.groupBy(Arrays.asList(singleRequest), writer::fdbMinutelyBucket);
    var avgReduction = readingGen.avgReduction(RES_TYPE.MINUTELY);
    assertEquals(avgReduction.getTimestamp().size(), groups.size());
    var map = avgReduction.asMap();
    var groupsAsMap =
        groups.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().bucket() * 60_000, e -> e.getValue().avg()));
    assertEquals(groupsAsMap, map);
  }

  @Test
  public void testSearchGroupBy_hourly() throws StatisticsFrozenException {
    // test with no values, multiple paths, single path -> should have the proper expected counts.
    var writer = fdbSingletonFactory.fdbWriter(node);
    var readingGen = new ReadingGenerator(Duration.of(1000, ChronoUnit.MILLIS), 120);
    var gen = readingGen.populateRandom(0.f, 100.f);
    var singleRequest =
        prototype
            .ts(OkapiLists.toLongArray(gen.getTimestamps()))
            .values(OkapiLists.toFloatArray(gen.getValues()))
            .build();

    var groups = writer.groupBy(Arrays.asList(singleRequest), writer::fdbHourlyBucket);
    var avgReduction = readingGen.avgReduction(RES_TYPE.HOURLY);
    assertEquals(avgReduction.getTimestamp().size(), groups.size());
    var map = avgReduction.asMap();
    var groupsAsMap =
        groups.entrySet().stream()
            .collect(
                Collectors.toMap(e -> e.getKey().bucket() * 3600_000L, e -> e.getValue().avg()));
    assertEquals(groupsAsMap, map);
  }

  @Test
  public void testSecondlyTwoMetricPaths() throws StatisticsFrozenException {
    var writer = fdbSingletonFactory.fdbWriter(node);
    var readingGen = new ReadingGenerator(Duration.of(1000, ChronoUnit.MILLIS), 120);
    var gen = readingGen.populateRandom(0.f, 100.f);
    var readingGen2 = new ReadingGenerator(Duration.of(1000, ChronoUnit.MILLIS), 120);
    var gen2 = readingGen2.populateRandom(0.f, 100.f);
    var request1 =
        prototype
            .ts(OkapiLists.toLongArray(gen.getTimestamps()))
            .values(OkapiLists.toFloatArray(gen.getValues()))
            .build();
    var request2 =
        prototype2
            .ts(OkapiLists.toLongArray(gen2.getTimestamps()))
            .values(OkapiLists.toFloatArray(gen2.getValues()))
            .build();
    var groups = writer.groupBy(Arrays.asList(request1, request2), writer::fdbHourlyBucket);
    // get the averages
    var avgReduction1 = readingGen.avgReduction(RES_TYPE.HOURLY);
    var avgReduction2 = readingGen2.avgReduction(RES_TYPE.HOURLY);
    var group1 =
        groups.entrySet().stream()
            .filter(s -> s.getKey().ts().equals(MetricPaths.convertToPath(request1)))
            .collect(
                Collectors.toMap(e -> e.getKey().bucket() * 3600_000L, e -> e.getValue().avg()));
    var group2 =
        groups.entrySet().stream()
            .filter(s -> s.getKey().ts().equals(MetricPaths.convertToPath(request2)))
            .collect(
                Collectors.toMap(e -> e.getKey().bucket() * 3600_000L, e -> e.getValue().avg()));
    assertEquals(group1, avgReduction1.asMap());
    assertEquals(group2, avgReduction2.asMap());
  }
}
