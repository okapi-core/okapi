package org.okapi.metrics.rollup;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.okapi.metrics.rollup.RollupTestUtils.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.okapi.clock.SystemClock;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.RocksDbStatsWriter;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.WriteBackRequest;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.query.QueryRecords;
import org.okapi.metrics.rocks.RocksPathSupplier;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.stats.*;

@Execution(ExecutionMode.CONCURRENT)
public class RolledupStatisticsTest {

  public static final Integer SHARD = 0;
  StatisticsRestorer<Statistics> statsRestorer;
  Supplier<Statistics> statisticsSupplier;
  Function<Integer, RollupSeries<Statistics>> seriesFunction;
  RocksPathSupplier rocksPathSupplier;
  @TempDir Path rocksDir;
  SharedMessageBox<WriteBackRequest> messageBox;
  RocksDbStatsWriter statsWriter;
  ScheduledExecutorService sch;
  RocksStore rocksStore;
  RocksReaderSupplier rocksReaderSupplier;
  WriteBackSettings writeBackSettings;

  @BeforeEach
  public void setupSeries() throws IOException {
    seriesFunction = new RollupSeriesFn();
    statsRestorer = new RolledupStatsRestorer();
    statisticsSupplier = new KllStatSupplier();
    rocksPathSupplier = new RocksPathSupplier(rocksDir);
    messageBox = new SharedMessageBox<>(10000);
    sch = Executors.newScheduledThreadPool(2);
    statsWriter =
        new RocksDbStatsWriter(
            messageBox, statsRestorer, new RolledupMergerStrategy(), rocksPathSupplier);
    rocksStore = new RocksStore();
    writeBackSettings = new WriteBackSettings(Duration.of(100, ChronoUnit.MILLIS), new SystemClock());
    statsWriter.startWriting(sch, rocksStore, writeBackSettings);
    rocksReaderSupplier = new RocksReaderSupplier(rocksPathSupplier, statsRestorer, rocksStore);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5, 10, 15, 30, 60, 120})
  public void testRollupCalculatesCorrectStats(int mins)
      throws StatisticsFrozenException, InterruptedException, IOException {
    // todo: take a break -> rework the failing tests.
    var ctx = new MetricsContext("test");
    var rollupStatistics = seriesFunction.apply(SHARD);
    var reading = new ReadingGenerator(Duration.of(10, ChronoUnit.MILLIS), mins);
    reading.populateRandom(500.f, 540.f);
    var ts = reading.getTimestamps();
    var vals = reading.getValues();
    for (int i = 0; i < ts.size(); i++) {
      rollupStatistics.write(ctx, "SeriesA", ts.get(i), vals.get(i));
    }
    // connect rollupStatistics to a shared message box
    rollupStatistics.startFreezing(messageBox, sch, writeBackSettings);
    await()
        .atMost(Duration.of(2, ChronoUnit.SECONDS))
        .until(
            () -> {
              return messageBox.isEmpty();
            });

    // setup query processing
    var path = rocksPathSupplier.apply(SHARD);

    // wait for the path to be written out
    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .pollDelay(Duration.of(10, ChronoUnit.MILLIS))
        .until(() -> rocksStore.rocksReader(path).isPresent());

    var reader = rocksStore.rocksReader(path);
    assertTrue(reader.isPresent());
    // apply reductions
    for (var reduction : AGG_TYPE.values()) {
      for (var resType : Arrays.asList(RES_TYPE.SECONDLY, RES_TYPE.MINUTELY, RES_TYPE.HOURLY)) {
        var reduced = applyReduction(reading, reduction, resType);
        for (int i = 0; i < reduced.getValues().size(); i++) {
          var expected = reduced.getValues().get(i);
          var time = reduced.getTimestamp().get(i);
          var val =
              getStats(new RocksReader(reader.get(), statsRestorer), "SeriesA", time, resType);
          // this is to speed up the test, if the first reading is not found, then we backoff until
          // a reading is obtained
          // this ensures that keys which are lazily synced are also available
          // without doing a direct read first, the tests slow down unnecessarily
          if (val.isEmpty()) {
            await()
                .atMost(Duration.of(1, ChronoUnit.SECONDS))
                .pollDelay(Duration.of(10, ChronoUnit.MILLIS))
                .until(
                    () -> {
                      return getStats(
                              new RocksReader(reader.get(), statsRestorer),
                              "SeriesA",
                              time,
                              resType)
                          .isPresent();
                    });
          }
          val = getStats(new RocksReader(reader.get(), statsRestorer), "SeriesA", time, resType);
          assertTrue(val.isPresent());
          var actual = getStats(val.get(), reduction);
          assertTolerableError(expected, actual, reduction, resType);
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5, 10, 15, 30, 60, 120})
  public void testScanAfterWrite(int mins)
      throws StatisticsFrozenException, InterruptedException, IOException {
    var ctx = new MetricsContext("test");
    var rollupStatistics = seriesFunction.apply(0);
    var reading = new ReadingGenerator(Duration.of(10, ChronoUnit.MILLIS), mins);
    reading.populateRandom(500.f, 540.f);
    var ts = reading.getTimestamps();
    var vals = reading.getValues();
    for (int i = 0; i < ts.size(); i++) {
      rollupStatistics.write(ctx, "SeriesA", ts.get(i), vals.get(i));
    }
    // setup query processing
    // connect rollupStatistics to a shared message box
    var writeBackSettings =
        new WriteBackSettings(Duration.of(100, ChronoUnit.MILLIS), new SystemClock());
    rollupStatistics.startFreezing(messageBox, sch, writeBackSettings);

    await().atMost(Duration.of(2, ChronoUnit.SECONDS)).until(() -> messageBox.isEmpty());
    // wait for the path to be written out
    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .pollDelay(Duration.of(10, ChronoUnit.MILLIS))
        .until(() -> rocksReaderSupplier.apply(SHARD).isPresent());

    // setup query processing
    var optionalRocksReader = rocksReaderSupplier.apply(SHARD);
    assertTrue(optionalRocksReader.isPresent());
    var reader = optionalRocksReader.get();
    var queryProcessor = new RollupQueryProcessor();
    // apply reductions
    for (var reduction : AGG_TYPE.values()) {
      for (var resType : Arrays.asList(RES_TYPE.SECONDLY, RES_TYPE.MINUTELY, RES_TYPE.HOURLY)) {
        var reduced = applyReduction(reading, reduction, resType);
        var scan =
            queryProcessor.scan(
                reader,
                new QueryRecords.Slice(
                    "SeriesA",
                    reduced.getTimestamp().getFirst(),
                    reduced.getTimestamp().getLast(),
                    resType,
                    reduction));
        var isEqual = reduced.getTimestamp().size() == scan.timestamps().size();
        // backoff if not equal
        if (!isEqual) {
          await()
              .atMost(Duration.of(1, ChronoUnit.SECONDS))
              .until(
                  () -> {
                    return queryProcessor
                            .scan(
                                reader,
                                new QueryRecords.Slice(
                                    "SeriesA",
                                    reduced.getTimestamp().getFirst(),
                                    reduced.getTimestamp().getLast(),
                                    resType,
                                    reduction))
                            .timestamps()
                            .size()
                        == reduced.getTimestamp().size();
                  });
          scan =
              queryProcessor.scan(
                  reader,
                  new QueryRecords.Slice(
                      "SeriesA",
                      reduced.getTimestamp().getFirst(),
                      reduced.getTimestamp().getLast(),
                      resType,
                      reduction));
        }
        assertEquals(
            reduced.getTimestamp(),
            scan.timestamps(),
            "Timestamps should match for operation: " + reduction + " and resolution: " + resType);
        assertTolerableError(reduced.getValues(), scan.values(), reduction, resType);
      }
    }
  }

  @AfterEach
  public void tearDown() throws IOException {
    this.statsWriter.close();
  }
}
