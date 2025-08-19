package org.okapi.metricsproxy.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.okapi.collections.OkapiLists;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.CheckpointUploaderDownloader;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.rollup.FrozenMetricsUploader;
import org.okapi.metrics.rollup.RollupSeries;
import org.okapi.metricsproxy.MetricsProxyConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = {MetricsProxyConfiguration.class, MetricsProxyTestConfig.class})
@ActiveProfiles("test")
public class ScanQueryProcessorTests {
  String tenantId = "queryProcessorTenant";

  @Autowired ScanQueryProcessor queryProcessor;
  @Autowired FrozenMetricsUploader frozenMetricsUploader;
  @Autowired CheckpointUploaderDownloader checkpointUploaderDownloader;
  @Autowired MetadataCache metadataCache;

  @Test
  public void testLoads() {}

  @Test
  public void testHourlyProcessing_singleShard() throws Exception {
    var series = new RollupSeries();
    var tsA = "ts-A-" + UUID.randomUUID().toString();
    var reading = new ReadingGenerator(Duration.of(1, ChronoUnit.SECONDS), 1);
    var gen = reading.populateRandom(1.f, 10.f);
    var path = MetricPaths.convertToPath(tenantId, tsA, Map.of("key1", "value1"));

    series.writeBatch(
        new MetricsContext("test"),
        path,
        OkapiLists.toLongArray(gen.getTimestamps()),
        OkapiLists.toFloatArray(gen.getValues()));

    var hourlyPath = Files.createTempFile("hourly", ".ckpt");
    var hour = gen.getTimestamps().get(0) / 1000 / 3600;
    frozenMetricsUploader.writeCheckpoint(tenantId, series, hourlyPath, hour);
    checkpointUploaderDownloader.uploadHourlyCheckpoint(tenantId, hourlyPath, hour, 0);

    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(
            () -> {
              var matchingPrefix = metadataCache.getPrefix(path, hour, tenantId);
              return matchingPrefix.isPresent();
            });

    var maybeResponse =
        queryProcessor.processHourlyQuery(
            hour, tenantId, tsA, Map.of("key1", "value1"), RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    assertTrue(maybeResponse.isPresent());
    var response = maybeResponse.get();
    var avg = gen.avgReduction(RES_TYPE.SECONDLY);
    assertEquals(avg.getTimestamp(), response.getTimes());
    assertEquals(avg.getValues(), response.getValues());
  }

  @Test
  public void testQueries_singleShard_two_paths() throws Exception {
    var tsA = "ts-A" + UUID.randomUUID().toString();
    var tsB = "ts-B" + UUID.randomUUID().toString();
    var series = new RollupSeries();
    // first series
    var readingA = new ReadingGenerator(Duration.of(1, ChronoUnit.SECONDS), 1);
    var genA = readingA.populateRandom(1.f, 10.f);
    var pathA = MetricPaths.convertToPath(tenantId, tsA, Map.of("key1", "value1"));

    // first second series
    var readingB = new ReadingGenerator(Duration.of(100, ChronoUnit.MILLIS), 1);
    var genB = readingB.populateRandom(1.f, 10.f);
    var pathB = MetricPaths.convertToPath(tenantId, tsB, Map.of("key1", "value1"));

    series.writeBatch(
        new MetricsContext("test"),
        pathA,
        OkapiLists.toLongArray(genA.getTimestamps()),
        OkapiLists.toFloatArray(genA.getValues()));

    series.writeBatch(
        new MetricsContext("test"),
        pathB,
        OkapiLists.toLongArray(genB.getTimestamps()),
        OkapiLists.toFloatArray(genB.getValues()));

    var hourlyPath = Files.createTempFile("hourly", ".ckpt");
    var hour = genA.getTimestamps().get(0) / 1000 / 3600;
    frozenMetricsUploader.writeCheckpoint(tenantId, series, hourlyPath, hour);
    checkpointUploaderDownloader.uploadHourlyCheckpoint(tenantId, hourlyPath, hour, 1);

    var matchingPrefix = metadataCache.getPrefix(pathA, hour, tenantId);
    assertTrue(matchingPrefix.isPresent());

    var maybeResponseA =
        queryProcessor.processHourlyQuery(
            hour, tenantId, tsA, Map.of("key1", "value1"), RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    assertTrue(maybeResponseA.isPresent());
    var responseA = maybeResponseA.get();
    var avgA = genA.avgReduction(RES_TYPE.SECONDLY);
    assertEquals(avgA.getTimestamp(), responseA.getTimes());
    assertEquals(avgA.getValues(), responseA.getValues());

    var maybeResponseB =
        queryProcessor.processHourlyQuery(
            hour, tenantId, tsB, Map.of("key1", "value1"), RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    assertTrue(maybeResponseB.isPresent());
    var responseB = maybeResponseB.get();
    var avgB = genB.avgReduction(RES_TYPE.SECONDLY);
    assertEquals(avgB.getTimestamp(), responseB.getTimes());
    assertEquals(avgB.getValues(), responseB.getValues());
  }

  @Test
  public void testQueries_multipleShards_multiplePaths() throws Exception {
    var tsA = "ts-A" + UUID.randomUUID().toString();
    var tsB = "ts-B" + UUID.randomUUID().toString();
    var seriesA = new RollupSeries();
    // first series
    var readingA = new ReadingGenerator(Duration.of(1, ChronoUnit.SECONDS), 1);
    var genA = readingA.populateRandom(1.f, 10.f);
    var pathA = MetricPaths.convertToPath(tenantId, tsA, Map.of("key1", "value1"));

    // first second series
    var readingB = new ReadingGenerator(Duration.of(100, ChronoUnit.MILLIS), 1);
    var genB = readingB.populateRandom(1.f, 10.f);
    var pathB = MetricPaths.convertToPath(tenantId, tsB, Map.of("key1", "value1"));
    var seriesB = new RollupSeries();

    seriesA.writeBatch(
        new MetricsContext("test"),
        pathA,
        OkapiLists.toLongArray(genA.getTimestamps()),
        OkapiLists.toFloatArray(genA.getValues()));

    seriesB.writeBatch(
        new MetricsContext("test"),
        pathB,
        OkapiLists.toLongArray(genB.getTimestamps()),
        OkapiLists.toFloatArray(genB.getValues()));

    var hourlyPath = Files.createTempFile("hourly", ".ckpt");
    var hour = genA.getTimestamps().get(0) / 1000 / 3600;
    frozenMetricsUploader.writeCheckpoint(tenantId, seriesA, hourlyPath, hour);
    checkpointUploaderDownloader.uploadHourlyCheckpoint(tenantId, hourlyPath, hour, 0);

    frozenMetricsUploader.writeCheckpoint(tenantId, seriesB, hourlyPath, hour);
    checkpointUploaderDownloader.uploadHourlyCheckpoint(tenantId, hourlyPath, hour, 1);

    var matchingPrefix = metadataCache.getPrefix(pathA, hour, tenantId);
    assertTrue(matchingPrefix.isPresent());

    var maybeResponseA =
        queryProcessor.processHourlyQuery(
            hour, tenantId, tsA, Map.of("key1", "value1"), RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    assertTrue(maybeResponseA.isPresent());
    var responseA = maybeResponseA.get();
    var avgA = genA.avgReduction(RES_TYPE.SECONDLY);
    assertEquals(avgA.getTimestamp(), responseA.getTimes());
    assertEquals(avgA.getValues(), responseA.getValues());

    var maybeResponseB =
        queryProcessor.processHourlyQuery(
            hour, tenantId, tsB, Map.of("key1", "value1"), RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    assertTrue(maybeResponseB.isPresent());
    var responseB = maybeResponseB.get();
    var avgB = genB.avgReduction(RES_TYPE.SECONDLY);
    assertEquals(avgB.getTimestamp(), responseB.getTimes());
    assertEquals(avgB.getValues(), responseB.getValues());
  }
}
