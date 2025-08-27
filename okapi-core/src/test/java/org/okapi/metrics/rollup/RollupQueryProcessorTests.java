package org.okapi.metrics.rollup;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.okapi.clock.SystemClock;
import org.okapi.collections.OkapiLists;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.*;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.query.QueryRecords;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.stats.*;

@Execution(ExecutionMode.CONCURRENT)
public class RollupQueryProcessorTests {
  // fixed shard to use with this test
  public static final Integer SHARD = 0;
  // resources to create a series
  StatisticsRestorer<Statistics> statsRestorer;
  Supplier<Statistics> statisticsSupplier;
  RollupSeriesRestorer<Statistics> restorer;

  ReadingGenerator generator;
  ReadingGenerator generator2;
  RollupSeries<Statistics> rollupSeries;
  RollupQueryProcessor queryProcessor;
  Function<Integer, RollupSeries<Statistics>> seriesFunction;
  long series1Start;
  long series2Start;
  long series1End;
  long series2End;
  MetricsContext ctx;

  // query processing
  @TempDir Path hourlyRoot;
  @TempDir Path shardPkgRoot;
  @TempDir Path parquetRoot;
  @TempDir Path shardAssetsRoot;
  PathRegistry pathRegistry;
  RocksReaderSupplier readerSupplier;
  RocksStore rocksStore;

  // message box
  SharedMessageBox<WriteBackRequest> messageBox;
  ScheduledExecutorService scheduledExecutorService;

  // setup consumer
  RocksDbStatsWriter rocksDbStatsWriter;
  WriteBackSettings writeBackSettings;

  @BeforeEach
  public void setUp() throws StatisticsFrozenException, InterruptedException, IOException {
    seriesFunction = new RollupSeriesFn();
    ctx = new MetricsContext("test");
    generator = new ReadingGenerator(Duration.of(10, ChronoUnit.MILLIS), 20);
    generator.populateRandom(100.f, 110.f);
    statsRestorer = new RolledupStatsRestorer();
    statisticsSupplier = new KllStatSupplier();
    restorer = new RolledUpSeriesRestorer(seriesFunction);
    rollupSeries = seriesFunction.apply(SHARD);
    // connect the series to a rocks-db instance;
    scheduledExecutorService = Executors.newScheduledThreadPool(2);
    messageBox = new SharedMessageBox<>(1000);
    pathRegistry = new PathRegistryImpl(hourlyRoot, shardPkgRoot, parquetRoot, shardAssetsRoot);

    writeBackSettings =
        new WriteBackSettings(Duration.of(100, ChronoUnit.MILLIS), new SystemClock());
    // start freezer
    rollupSeries.startFreezing(messageBox, scheduledExecutorService, writeBackSettings);

    // start writer
    rocksStore = new RocksStore();
    rocksDbStatsWriter =
        new RocksDbStatsWriter(
            messageBox, statsRestorer, new RolledupMergerStrategy(), pathRegistry);
    rocksDbStatsWriter.startWriting(scheduledExecutorService, rocksStore, writeBackSettings);
    // test-dataset 1
    rollupSeries.writeBatch(
        ctx,
        "test_series",
        OkapiLists.toLongArray(generator.getTimestamps()),
        OkapiLists.toFloatArray(generator.getValues()));
    series1Start = generator.getTimestamps().getFirst();
    series1End = generator.getTimestamps().getLast();

    // test-dataset 2
    generator2 = new ReadingGenerator(Duration.of(30, ChronoUnit.MILLIS), 20);
    generator2.populateRandom(100.f, 110.f);
    rollupSeries.writeBatch(
        ctx,
        "test_series_2",
        OkapiLists.toLongArray(generator2.getTimestamps()),
        OkapiLists.toFloatArray(generator2.getValues()));
    series2Start = generator2.getTimestamps().getFirst();
    series2End = generator2.getTimestamps().getLast();

    // query processing
    queryProcessor = new RollupQueryProcessor();
    readerSupplier = new RocksReaderSupplier(pathRegistry, statsRestorer, rocksStore);

    // wait until the message box is empty
    await().atMost(Duration.of(5, ChronoUnit.SECONDS)).until(() -> messageBox.isEmpty());

    // wait until the database has been written to
    await()
        .atMost(Duration.of(5, ChronoUnit.SECONDS))
        .until(() -> readerSupplier.apply(SHARD).isPresent());
  }

  @Test
  public void testScanQuery() {
    var slice =
        new QueryRecords.Slice(
            "test_series", series1Start, series1End, RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    var reduction = generator.avgReduction(RES_TYPE.SECONDLY);
    var reader = readerSupplier.apply(SHARD);
    assertTrue(reader.isPresent());
    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(
            () ->
                queryProcessor.scan(reader.get(), slice).timestamps().size()
                    == reduction.getTimestamp().size());
    var scanResult = queryProcessor.scan(reader.get(), slice);
    assertEquals(reduction.getTimestamp(), scanResult.timestamps());
    assertEquals(reduction.getValues(), scanResult.values());
  }

  @Test
  public void testScale() {
    var slice =
        new QueryRecords.Slice(
            "test_series_2", series1Start, series1End, RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    var scaleFactor = 2.0f;
    var reader = readerSupplier.apply(SHARD);
    assertTrue(reader.isPresent());
    var reduction = generator2.avgReduction(RES_TYPE.SECONDLY);
    var expectedValues = reduction.getValues();
    await()
        .atMost(Duration.of(2, ChronoUnit.SECONDS))
        .until(
            () -> {
              var scan = queryProcessor.scan(reader.get(), slice);
              return scan.timestamps().size() == reduction.getTimestamp().size();
            });
    var scaledResult = queryProcessor.scale(reader.get(), slice, scaleFactor);
    for (int i = 0; i < expectedValues.size(); i++) {
      expectedValues.set(i, expectedValues.get(i) * scaleFactor);
    }
    assertEquals(reduction.getTimestamp(), scaledResult.timestamps());
  }

  @Test
  public void testSum() {
    var slice1 =
        new QueryRecords.Slice(
            "test_series", series1Start, series1End, RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    var slice2 =
        new QueryRecords.Slice(
            "test_series_2", series2Start, series2End, RES_TYPE.SECONDLY, AGG_TYPE.AVG);
    var reader = readerSupplier.apply(SHARD);
    assertTrue(reader.isPresent());
    var expectedTimestamps = generator.avgReduction(RES_TYPE.SECONDLY).getTimestamp();
    await()
        .atMost(Duration.of(2, ChronoUnit.SECONDS))
        .until(
            () -> {
              var result = queryProcessor.sum(reader.get(), slice1, slice2);
              return result.timestamps().size() == expectedTimestamps.size();
            });
    var sumResult = queryProcessor.sum(reader.get(), slice1, slice2);
    // slice1 and slice2 have different timestamps, so we need to merge them
    // convert these to a map, then sum up values in the same timestamp
    var expectedValues = generator.avgReduction(RES_TYPE.SECONDLY).getValues();
    var mapOfValues = new TreeMap<Long, Float>();
    for (int i = 0; i < expectedTimestamps.size(); i++) {
      mapOfValues.put(expectedTimestamps.get(i), expectedValues.get(i));
    }
    var secondTimestamps = generator2.avgReduction(RES_TYPE.SECONDLY).getTimestamp();
    var secondValues = generator2.avgReduction(RES_TYPE.SECONDLY).getValues();
    for (int i = 0; i < secondTimestamps.size(); i++) {
      mapOfValues.merge(
          secondTimestamps.get(i), secondValues.get(i), Float::sum); // sum values with same ts
    }

    var summedTimestamps = new ArrayList<>(mapOfValues.keySet());
    var summedValues = new ArrayList<Float>(mapOfValues.values());
    assertEquals(summedTimestamps, sumResult.timestamps());
    assertEquals(summedValues, sumResult.values());
  }

  @Test
  public void testMovingWindowTransform() throws StatisticsFrozenException, InterruptedException {
    var t0 = System.currentTimeMillis();
    var ts = Arrays.asList(t0, t0 + 2000, t0 + 3000, t0 + 5000);
    var values = Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f);
    rollupSeries.writeBatch(
        ctx, "test_series_1", OkapiLists.toLongArray(ts), OkapiLists.toFloatArray(values));
    var slice =
        new QueryRecords.Slice("test_series_1", t0, t0 + 6000, RES_TYPE.SECONDLY, AGG_TYPE.AVG);

    var reader = readerSupplier.apply(SHARD);
    assertTrue(reader.isPresent());
    var expectedTs =
        Stream.of(t0, t0 + 1000, t0 + 2000, t0 + 3000, t0 + 4000, t0 + 5000, t0 + 6000)
            .map(this::roundToSeconds)
            .toList();

    // wait until all results are synchronized
    await()
        .atMost(Duration.of(30, ChronoUnit.SECONDS))
        .until(
            () -> {
              var result =
                  queryProcessor.movingSum(reader.get(), slice, Duration.of(2, ChronoUnit.SECONDS));
              return result.timestamps().size() == expectedTs.size();
            });
    // setup query processing
    var movingSum =
        queryProcessor.movingSum(reader.get(), slice, Duration.of(2, ChronoUnit.SECONDS));
    var expectedVals = Arrays.asList(1.0f, 1.0f, 3.0f, 5.0f, 5.0f, 7.0f, 4.0f);
    assertEquals(expectedTs, movingSum.timestamps());
    assertEquals(expectedVals, movingSum.values());
  }

  @Test
  public void testMovingWindowAverage()
      throws OutsideWindowException, StatisticsFrozenException, InterruptedException {
    var t0 = System.currentTimeMillis();
    var ts = Arrays.asList(t0, t0 + 2000, t0 + 3000, t0 + 5000);
    var values = Arrays.asList(1.0f, 2.0f, 3.0f, 4.0f);
    rollupSeries.writeBatch(
        ctx, "test_series_1", OkapiLists.toLongArray(ts), OkapiLists.toFloatArray(values));
    var slice =
        new QueryRecords.Slice("test_series_1", t0, t0 + 6000, RES_TYPE.SECONDLY, AGG_TYPE.AVG);

    // setup query processing
    var reader = readerSupplier.apply(SHARD);
    assertTrue(reader.isPresent());
    var expectedTs =
        Stream.of(t0, t0 + 1000, t0 + 2000, t0 + 3000, t0 + 4000, t0 + 5000, t0 + 6000)
            .map(this::roundToSeconds)
            .toList();

    // wait until all results are synchronized
    await()
        .atMost(Duration.of(30, ChronoUnit.SECONDS))
        .until(
            () -> {
              var result =
                  queryProcessor.movingAverage(
                      reader.get(), slice, Duration.of(2, ChronoUnit.SECONDS));
              return result.timestamps().size() == expectedTs.size();
            });
    var movingAverage =
        queryProcessor.movingAverage(reader.get(), slice, Duration.of(2, ChronoUnit.SECONDS));
    var expectedVals = Arrays.asList(1.0f, 1.0f, 1.5f, 2.5f, 2.5f, 3.5f, 4.0f);
    assertEquals(expectedTs, movingAverage.timestamps());
    assertEquals(expectedVals, movingAverage.values());
  }

  public long roundToSeconds(long timestamp) {
    return 1000 * (timestamp / 1000);
  }
}
