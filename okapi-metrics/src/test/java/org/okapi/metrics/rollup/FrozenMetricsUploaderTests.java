package org.okapi.metrics.rollup;

import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.metrics.GlobalTestConfig.okapiWait;

import com.google.common.primitives.Longs;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.avro.MetricRow;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.common.ZkPaths;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.s3.S3Prefixes;
import org.okapi.metrics.scanning.ArrayBackedBrs;
import org.okapi.metrics.scanning.HourlyCheckpointScanner;
import org.okapi.metrics.scanning.MmapBrs;
import org.okapi.metrics.stats.*;
import org.okapi.testutils.OkapiTestUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class FrozenMetricsUploaderTests {
  public static String TENANT_ID;
  public static String TENANT_ID_2;
  TestResourceFactory testResourceFactory;
  Duration oneMin = Duration.of(1, ChronoUnit.MINUTES);
  Duration oneHr = Duration.of(1, ChronoUnit.HOURS);
  long now = 0L;
  long hr = -1L;
  Node node;

  Function<Integer, RollupSeries<Statistics>> seriesSupplier;
  StatisticsRestorer<Statistics> statsRestorer;
  Supplier<Statistics> statisticsSupplier;
  RollupSeriesRestorer<Statistics> restorer;

  // test artifacts
  public static final String PATH = "series{}";
  public static final String PATH_SERIES_1 = "series-1{}";
  public static final String PATH_SERIES_2 = "series-2{}";

  @BeforeEach
  public void setup() {
    testResourceFactory = new TestResourceFactory();
    testResourceFactory.setUseRealClock(true);
    hr = System.currentTimeMillis() / (3600_000);
    now = hr * 3600_000;
    TENANT_ID = "test" + System.currentTimeMillis();
    TENANT_ID_2 = "test" + System.currentTimeMillis();
    node =
        new Node(
            "test-node-" + UUID.randomUUID().toString(),
            "localhost",
            NodeState.METRICS_CONSUMPTION_START);
    statsRestorer = new RolledupStatsRestorer();
    statisticsSupplier = new KllStatSupplier();
    seriesSupplier = new RollupSeriesFn();
    restorer = new RolledUpSeriesRestorer(seriesSupplier);
  }

  @Test
  public void testHourlyWithOneSeries() throws Exception {
    var shardMap = testResourceFactory.shardMap(node);
    var checkpointWriter = testResourceFactory.hourlyCheckpointWriter(node);
    shardMap.apply(
        0,
        new MetricsContext("ctx"),
        tenantize(PATH),
        new long[] {now + 1000L, now + 2000L, now + 3000L},
        new float[] {1.0f, 2.0f, 3.0f});

    waitUntilFlushed(Arrays.asList(0));
    waitForWritesSync(checkpointWriter, Arrays.asList(TENANT_ID), hr);
    var fp = Files.createTempFile("hourly", ".ckpt");
    checkpointWriter.writeCheckpoint(TENANT_ID, fp, hr);

    var scanner = new HourlyCheckpointScanner();
    var fileRange = new MmapBrs(fp.toFile());
    var metrics = scanner.listMetrics(fileRange);
    assertTrue(metrics.contains(tenantize(PATH)));
    var md = scanner.getMd(fileRange);
    assertTrue(md.containsKey(tenantize(PATH)));
    var secondly = scanner.secondly(fileRange, tenantize(PATH), md);
    assertEquals(3, secondly.getTs().size());
    assertEquals(Arrays.asList(1, 2, 3), secondly.getTs());

    // get a reader for shard 0
    var store = testResourceFactory.rocksStore(node);
    var dbPath = testResourceFactory.pathRegistry(node).rocksPath(0);
    var reader = new RocksTsReader(store.rocksReader(dbPath).get(), new RolledupStatsRestorer());

    assertTrue(
        OkapiTestUtils.bytesAreEqual(
            reader.secondlyStats(tenantize(PATH), now + 1000).get().serialize(),
            secondly.getVals().get(0).serialize()));
    assertTrue(
        OkapiTestUtils.bytesAreEqual(
            reader.secondlyStats(tenantize(PATH), now + 2000).get().serialize(),
            secondly.getVals().get(1).serialize()));
    assertTrue(
        OkapiTestUtils.bytesAreEqual(
            reader.secondlyStats(tenantize(PATH), now + 3000).get().serialize(),
            secondly.getVals().get(2).serialize()));

    var minutely = scanner.minutely(fileRange, tenantize(PATH), md);
    assertEquals(1, minutely.getTs().size());
    var expectedMinVal = reader.minutelyStats(tenantize(PATH), now + 1000).get().serialize();
    var gotMinVal = minutely.getVals().get(0).serialize();
    assertTrue(OkapiTestUtils.bytesAreEqual(expectedMinVal, gotMinVal));

    var hourly = scanner.hourly(fileRange, tenantize(PATH), md);
    var expectedHourlyVal = reader.hourlyStats(tenantize(PATH), now + 1000).get().serialize();
    var gotHrVal = hourly.serialize();
    assertTrue(OkapiTestUtils.bytesAreEqual(expectedHourlyVal, gotHrVal));
  }

  private void waitUntilFlushed(List<Integer> shards) throws IOException {
    // check that message box is synchronized
    okapiWait()
        .until(
            () -> {
              return !testResourceFactory.messageBox(node).isEmpty();
            });

    // start writer
    testResourceFactory.startWriter(node);
    okapiWait()
        .until(
            () -> {
              return testResourceFactory.messageBox(node).isEmpty();
            });

    for (var shard : shards) {
      okapiWait()
          .until(
              () -> {
                var path = testResourceFactory.pathRegistry(node).rocksPath(shard);
                var reader = testResourceFactory.rocksStore(node).rocksReader(path);
                return reader.isPresent();
              });
    }
  }

  public void waitForWritesSync(
      FrozenMetricsUploader metricsUploader, List<String> tenants, long hr) {
    for (var tenant : tenants) {
      okapiWait()
          .until(
              () -> {
                var fp = Files.createTempFile("test", "ckpt");
                metricsUploader.writeCheckpoint(tenant, fp, hr);
                return Files.size(fp) > 0;
              });
    }
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @MethodSource("testHourlyWithMultipleSeriesSource")
  public void testHourlyWithMultipleSeries(
      List<String> names, List<long[]> ts, List<float[]> vals, long hr) throws Exception {
    var checkpointWriter = testResourceFactory.hourlyCheckpointWriter(node);
    int n = names.size();
    var shardMap = testResourceFactory.shardMap(node);
    for (int i = 0; i < n; i++) {
      shardMap.apply(0, new MetricsContext("ctx"), tenantize(names.get(i)), ts.get(i), vals.get(i));
    }

    // wait until a checkpoint shows up
    waitUntilFlushed(Arrays.asList(0));
    waitForWritesSync(checkpointWriter, Arrays.asList(TENANT_ID), hr);
    var fp = Files.createTempFile("hourly", ".ckpt");
    checkpointWriter.writeCheckpoint(TENANT_ID, fp, hr);

    var scanner = new HourlyCheckpointScanner();
    var fileRange = new MmapBrs(fp.toFile());
    var metrics = scanner.listMetrics(fileRange);
    var md = scanner.getMd(fileRange);

    // get a reader for reference
    var store = testResourceFactory.rocksStore(node);
    var dbPath = testResourceFactory.pathRegistry(node).rocksPath(0);
    var reader = new RocksTsReader(store.rocksReader(dbPath).get(), new RolledupStatsRestorer());

    for (int i = 0; i < n; i++) {
      var name = tenantize(names.get(i));
      assertTrue(metrics.contains(name));
      assertTrue(md.containsKey(name));
      {
        var secondly = scanner.secondly(fileRange, name, md);
        var tsIter = secondly.getTs().iterator();
        var valIter = secondly.getVals().iterator();
        while (tsIter.hasNext()) {
          var secondlyTs = (now + tsIter.next() * 1000L);
          var val = reader.secondlyStats(name, secondlyTs).get().serialize();
          var got = valIter.next().serialize();
          assertTrue(OkapiTestUtils.bytesAreEqual(val, got));
        }
      }
      {
        var minutely = scanner.minutely(fileRange, name, md);
        var tsIter = minutely.getTs().iterator();
        var valIter = minutely.getVals().iterator();
        while (tsIter.hasNext()) {
          var minutelyTs = (now + tsIter.next() * 60_000L);
          var val = reader.minutelyStats(name, minutelyTs).get().serialize();
          var got = valIter.next().serialize();
          assertTrue(OkapiTestUtils.bytesAreEqual(val, got));
        }
      }
      {
        var hourly = scanner.hourly(fileRange, name, md);
        var expectedHourly = reader.hourlyStats(name, now).get().serialize();
        assertTrue(OkapiTestUtils.bytesAreEqual(expectedHourly, hourly.serialize()));
      }
    }
  }

  public void writeBackdatedData(FrozenMetricsUploader checkpointer, Duration writeStart)
      throws StatisticsFrozenException, InterruptedException, IOException {
    var shardMap = testResourceFactory.shardMap(node);
    var st = now - writeStart.toMillis();
    shardMap.forciblyApply(
        0,
        new MetricsContext("ctx"),
        tenantize(PATH_SERIES_1),
        new long[] {
          st + oneMin.toMillis(),
          st + 2 * oneMin.toMillis(),
          st + oneHr.toMillis(),
          st + 2 * oneHr.toMillis(),
        },
        new float[] {1.0f, 2.0f, 3.0f, 4.0f});
    shardMap.forciblyApply(
        1,
        new MetricsContext("ctx"),
        tenantize("series-1-neg"),
        new long[] {
          st + oneMin.toMillis(),
          st + 2 * oneMin.toMillis(),
          st + oneHr.toMillis(),
          st + 2 * oneHr.toMillis(),
        },
        new float[] {-1.0f, -2.0f, -3.0f, -4.0f});
    shardMap.forciblyApply(
        0,
        new MetricsContext("ctx"),
        tenantize(PATH_SERIES_2),
        new long[] {
          st + 3 * oneMin.toMillis(),
          st + 4 * oneMin.toMillis(),
          st + oneHr.toMillis(),
          st + 2 * oneHr.toMillis(),
        },
        new float[] {11.0f, 12.0f, 13.0f, 14.0f});
    waitUntilFlushed(Arrays.asList(0));
    waitForWritesSync(checkpointer, Arrays.asList(TENANT_ID), st / 3600_000);
  }

  @Test
  public void testHourlyUpload_withoutPrevious() throws Exception {
    var admissionWindowHrs = 6;
    testResourceFactory.setAdmissionWindowHrs(6);
    var checkpointWriter = testResourceFactory.hourlyCheckpointWriter(node);
    var admissionWindow = Duration.of(admissionWindowHrs, ChronoUnit.HOURS);
    writeBackdatedData(checkpointWriter, admissionWindow);
    checkpointWriter.uploadHourlyCheckpoint();
    var s3Client = testResourceFactory.s3Client();
    var expectedHour = now / 1000 / 3600 - admissionWindowHrs;
    var prefix = S3Prefixes.hourlyPrefix(TENANT_ID, expectedHour, 0);
    var bytes =
        s3Client
            .getObject(
                GetObjectRequest.builder()
                    .bucket(testResourceFactory.getDataBucket())
                    .key(prefix)
                    .build())
            .readAllBytes();
    assertTrue(bytes.length > 0);

    // verify that the checkpoint is what we expect
    var brs = new ArrayBackedBrs(bytes);
    var hourlyScanner = new HourlyCheckpointScanner();
    var list = hourlyScanner.listMetrics(brs);
    assertEquals(Set.of(tenantize(PATH_SERIES_1), tenantize(PATH_SERIES_2)), list);
    var md = hourlyScanner.getMd(brs);

    // check secondly data
    var secondly = hourlyScanner.secondly(brs, tenantize(PATH_SERIES_1), md);
    assertEquals(2, secondly.getTs().size());
    assertEquals(Arrays.asList(60, 120), secondly.getTs());

    // get reader for shard-0
    // check secondly stats
    var reader0 = testResourceFactory.rocksReaderSupplier(node).apply(0).get();
    var expectedSecondly1 =
        reader0
            .secondlyStats(
                tenantize(PATH_SERIES_1), now + oneMin.toMillis() - admissionWindow.toMillis())
            .get();
    var expectedSecondly2 =
        reader0
            .secondlyStats(
                tenantize(PATH_SERIES_1), now + 2 * oneMin.toMillis() - admissionWindow.toMillis())
            .get();
    assertTrue(
        OkapiTestUtils.bytesAreEqual(
            expectedSecondly1.serialize(), secondly.getVals().get(0).serialize()));
    assertTrue(
        OkapiTestUtils.bytesAreEqual(
            expectedSecondly2.serialize(), secondly.getVals().get(1).serialize()));

    // check minutely stats
    var minutely = hourlyScanner.minutely(brs, tenantize(PATH_SERIES_1), md);
    assertEquals(2, minutely.getTs().size());
    assertEquals(Arrays.asList(1, 2), minutely.getTs());
    var expectedMinutely1 =
        reader0.minutelyStats(
            tenantize(PATH_SERIES_1), now + oneMin.toMillis() - admissionWindow.toMillis());
    var expectedMinutely2 =
        reader0.minutelyStats(
            tenantize(PATH_SERIES_1), now + 2 * oneMin.toMillis() - admissionWindow.toMillis());
    assertTrue(
        OkapiTestUtils.bytesAreEqual(
            expectedMinutely1.get().serialize(), minutely.getVals().get(0).serialize()));
    assertTrue(
        OkapiTestUtils.bytesAreEqual(
            expectedMinutely2.get().serialize(), minutely.getVals().get(1).serialize()));

    // check hourly stats
    var expectedHourly =
        reader0.hourlyStats(tenantize(PATH_SERIES_1), now - admissionWindow.toMillis()).get();
    var hourly = hourlyScanner.hourly(brs, tenantize(PATH_SERIES_1), md);
    assertTrue(OkapiTestUtils.bytesAreEqual(expectedHourly.serialize(), hourly.serialize()));

    // check that ZK state was updated
    var zkState =
        testResourceFactory.fleetMetadata().getData(ZkPaths.lastCheckpointedHour(node.id()));
    assertNotNull(zkState);
    var expectedPrev = (now - admissionWindow.toMillis()) / 1000L / 3600;
    assertEquals(expectedPrev, Longs.fromByteArray(zkState));
  }

  @Test
  public void testHourlyUpload_withPrevious() throws Exception {
    // use a 23hr admissions window to simulate the case where the checkpointing is lagging
    testResourceFactory.setAdmissionWindowHrs(23);
    var frozenWriter = testResourceFactory.hourlyCheckpointWriter(node);
    // data is 25 hrs old, data spans two hours -> 25hr old, 24hr old and 23hr old
    writeBackdatedData(frozenWriter, Duration.of(25, ChronoUnit.HOURS));
    var checkpointPath = Files.createTempFile("expected", "ckpt");
    // we update last checkpoint to be 24 hr old
    var hr = (now - 24 * oneHr.toMillis()) / 1000L / 3600;
    testResourceFactory.nodeStateRegistry(node).updateLastCheckPointedHour(hr);
    // this time we write data that is 23hrs old
    frozenWriter.uploadHourlyCheckpoint();
    var s3 = testResourceFactory.s3Client();
    // we expect a checkpoint for 1 + hr to be written
    var prefix = S3Prefixes.hourlyPrefix(TENANT_ID, 1 + hr, 0);
    var bytes =
        s3.getObject(
                GetObjectRequest.builder()
                    .bucket(testResourceFactory.getDataBucket())
                    .key(prefix)
                    .build())
            .readAllBytes();
    assertTrue(bytes.length > 0);
    testResourceFactory
        .hourlyCheckpointWriter(node)
        .writeCheckpoint(TENANT_ID, checkpointPath, 1 + hr);
    assertTrue(OkapiTestUtils.bytesAreEqual(bytes, Files.readAllBytes(checkpointPath)));
    var lastHr = testResourceFactory.nodeStateRegistry(node).getLastCheckpointedHour();
    assertEquals(1 + hr, lastHr.get());
  }

  @Test
  public void testHourly_withSkipped() throws Exception {
    testResourceFactory.setAdmissionWindowHrs(24);
    var writer = testResourceFactory.hourlyCheckpointWriter(node);
    writeBackdatedData(writer, Duration.of(24, ChronoUnit.HOURS));
    var toWrite = (now - 24 * oneHr.toMillis()) / 1000 / 3600;
    testResourceFactory.nodeStateRegistry(node).updateLastCheckPointedHour(toWrite - 1);
    // should write a checkpoint for hr: now - 24Hrs, instead of now - 24hrs + 3hrs
    testResourceFactory.hourlyCheckpointWriter(node).uploadHourlyCheckpoint();
    var temp1 = Files.createTempFile("temp1", ".tmp");
    writer.writeCheckpoint(TENANT_ID, temp1, toWrite);
    var temp2 = Files.createTempFile("temp1", ".tmp");
    writer.writeCheckpoint(TENANT_ID, temp2, toWrite);
    OkapiTestUtils.bytesAreEqual(Files.readAllBytes(temp1), Files.readAllBytes(temp2));
    var prefix = S3Prefixes.hourlyPrefix(TENANT_ID, toWrite, 0);
    var bytes =
        testResourceFactory
            .s3Client()
            .getObject(
                GetObjectRequest.builder()
                    .bucket(testResourceFactory.getDataBucket())
                    .key(prefix)
                    .build())
            .readAllBytes();
    assertTrue(bytes.length > 0);

    var expectedCheckpointPath = Files.createTempFile("expected", "ckpt");
    testResourceFactory
        .hourlyCheckpointWriter(node)
        .writeCheckpoint(TENANT_ID, expectedCheckpointPath, toWrite);
    assertTrue(OkapiTestUtils.bytesAreEqual(bytes, Files.readAllBytes(expectedCheckpointPath)));
    var lastHr = testResourceFactory.nodeStateRegistry(node).getLastCheckpointedHour();
    assertEquals(toWrite, lastHr.get());
  }

  @Test
  public void testParquetUpload_single_shard() throws Exception {
    // cases : single shard, multiple shards, multiple tenants.
    var admissionWindowHrs = 6;
    testResourceFactory.setAdmissionWindowHrs(admissionWindowHrs);
    var shardMap = testResourceFactory.shardMap(node);
    // rewind the clock
    var start = now - admissionWindowHrs * oneHr.toMillis();
    shardMap.forciblyApply(
        0,
        MetricsContext.createContext("test"),
        tenantize(TENANT_ID, PATH_SERIES_1),
        new long[] {start, start + 100, start + 200},
        new float[] {1.0f, 2.0f, 3.0f});
    waitUntilFlushed(Arrays.asList(0));

    // do an upload
    var checkpointer = testResourceFactory.hourlyCheckpointWriter(node);
    checkpointer.uploadHourlyCheckpoint();

    // check that parquet is written out
    var epoch = now / 1000 / 3600 - admissionWindowHrs;
    var s3 = testResourceFactory.s3Client();
    var tempFile = Files.createTempFile("dump", ".parquet");
    try (var fos = new FileOutputStream(tempFile.toFile())) {
      s3.getObject(
              GetObjectRequest.builder()
                  .bucket(testResourceFactory.getDataBucket())
                  .key(S3Prefixes.parquetPrefix(TENANT_ID, epoch))
                  .build())
          .transferTo(fos);
      fos.getChannel().force(true);
    }
    checkParquet(tempFile);
  }

  @Test
  public void testParquetUpload_singleShardMultipleTenants() throws Exception {
    // cases : single shard, multiple shards, multiple tenants.
    var admissionWindowHrs = 6;
    testResourceFactory.setAdmissionWindowHrs(admissionWindowHrs);
    var shardMap = testResourceFactory.shardMap(node);
    // rewind the clock
    var t = now - admissionWindowHrs * oneHr.toMillis();
    shardMap.forciblyApply(
        0,
        MetricsContext.createContext("test"),
        tenantize(TENANT_ID, PATH_SERIES_1),
        new long[] {t, t + 100, t + 200},
        new float[] {1.0f, 2.0f, 3.0f});

    shardMap.forciblyApply(
        1,
        MetricsContext.createContext("test"),
        tenantize(TENANT_ID_2, PATH_SERIES_1),
        new long[] {t, t + 100, t + 200},
        new float[] {1.0f, 2.0f, 3.0f});
    waitUntilFlushed(Arrays.asList(0, 1));
    // do the upload
    var checkpointer = testResourceFactory.hourlyCheckpointWriter(node);
    checkpointer.uploadHourlyCheckpoint();

    // check for both tenants
    // check that parquet is written out
    for (var tenant : Arrays.asList(TENANT_ID, TENANT_ID_2)) {
      var epoch = now / 1000 / 3600 - admissionWindowHrs;
      var s3 = testResourceFactory.s3Client();
      var tempFile = Files.createTempFile("dump", ".parquet");
      try (var fos = new FileOutputStream(tempFile.toFile())) {
        s3.getObject(
                GetObjectRequest.builder()
                    .bucket(testResourceFactory.getDataBucket())
                    .key(S3Prefixes.parquetPrefix(tenant, epoch))
                    .build())
            .transferTo(fos);
        fos.getChannel().force(true);
      }
      checkParquet(tempFile);
    }
  }

  public void checkParquet(java.nio.file.Path fp) throws IOException {
    Configuration conf = new Configuration();
    Path parquetPath = new Path(fp.toAbsolutePath().toString());
    // Use HadoopInputFile instead of Path
    var inputFile = HadoopInputFile.fromPath(parquetPath, conf);
    try (var parquetReader = AvroParquetReader.<MetricRow>builder(inputFile).build()) {
      MetricRow record;
      while ((record = parquetReader.read()) != null) {
        // todo: test out thoroughly that the metrics match, for now we accept that parseable
        // parquet is valid.
        System.out.println(record);
      }
    }
  }

  public List<Integer> minSub(long[] ts, int quantizer) {
    var m = Long.MAX_VALUE;
    for (int i = 0; i < ts.length; i++) {
      m = Math.min(m, ts[i]);
    }
    var hr = (m / 1000 / 3600) * 1000 * 3600;
    var arr = new ArrayList<Integer>();
    for (int i = 0; i < ts.length; i++) {
      var relativeNumer = (ts[i] - hr) / quantizer;
      if (arr.contains((int) relativeNumer)) continue;
      arr.add((int) relativeNumer);
    }
    return arr;
  }

  public static Stream<Arguments> testHourlyWithMultipleSeriesSource() {
    final var q = 1000 * 3600;
    var t0 = q * (System.currentTimeMillis() / q) + 1234;
    return Stream.of(
        Arguments.of(
            Arrays.asList(PATH),
            Arrays.asList(new long[] {t0 + 1000, t0 + 2000, t0 + 3000}),
            Arrays.asList(new float[] {1.f, 2.f, 3.f}),
            t0 / q),
        Arguments.of(
            Arrays.asList(PATH, PATH_SERIES_2),
            Arrays.asList(
                new long[] {t0 + 1000, t0 + 2000, t0 + 3000},
                new long[] {t0 + 1000, t0 + 2000, t0 + 3000}),
            Arrays.asList(new float[] {1.f, 2.f, 3.f}, new float[] {11.f, 12.f, 13.f}),
            t0 / q),
        Arguments.of(
            Arrays.asList(PATH, PATH_SERIES_2),
            Arrays.asList(
                new long[] {
                  t0 + Duration.of(1, ChronoUnit.MINUTES).toMillis(),
                  t0 + Duration.of(2, ChronoUnit.MINUTES).toMillis(),
                  t0 + Duration.of(3, ChronoUnit.MINUTES).toMillis(),
                },
                new long[] {
                  t0 + Duration.of(4, ChronoUnit.MINUTES).toMillis(),
                  t0 + Duration.of(5, ChronoUnit.MINUTES).toMillis(),
                  t0 + Duration.of(10, ChronoUnit.MINUTES).toMillis(),
                }),
            Arrays.asList(new float[] {1.f, 2.f, 3.f}, new float[] {11.f, 12.f, 13.f}),
            t0 / q),
        Arguments.of(
            Arrays.asList(PATH, PATH_SERIES_2),
            Arrays.asList(
                new long[] {
                  t0 + Duration.of(11, ChronoUnit.MILLIS).toMillis(),
                  t0 + Duration.of(12, ChronoUnit.MILLIS).toMillis(),
                  t0 + Duration.of(20, ChronoUnit.SECONDS).toMillis(),
                  t0 + Duration.of(2, ChronoUnit.MINUTES).toMillis(),
                  t0 + Duration.of(3, ChronoUnit.MINUTES).toMillis(),
                },
                new long[] {
                  t0 + Duration.of(20, ChronoUnit.SECONDS).toMillis(),
                  t0 + Duration.of(4, ChronoUnit.MINUTES).toMillis(),
                  t0 + Duration.of(5, ChronoUnit.MINUTES).toMillis(),
                  t0 + Duration.of(10, ChronoUnit.MINUTES).toMillis(),
                }),
            Arrays.asList(
                new float[] {1.f, 2.f, 3.f, 4.f, 5.f}, new float[] {11.f, 12.f, -13.f, 11015.f}),
            t0 / q));
  }

  private static String tenantize(String ts) {
    return TENANT_ID + ":" + ts;
  }

  private static String tenantize(String tenant, String ts) {
    return tenant + ":" + ts;
  }

  public void printBytes(byte[] A) {
    System.out.print("[ ");
    for (var b : A) {
      System.out.print(b);
      System.out.print(" ");
    }
    System.out.print(" ]");
    System.out.println();
  }
}
