package org.okapi.metrics.rollup;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.primitives.Longs;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.okapi.clock.Clock;
import org.okapi.fake.FakeClock;
import org.okapi.metrics.OutsideWindowException;
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
  long now =
      Instant.from(LocalDateTime.of(2025, 7, 8, 12, 0, 0).atZone(ZoneOffset.UTC)).toEpochMilli();
  Duration oneMin = Duration.of(1, ChronoUnit.MINUTES);
  Duration oneHr = Duration.of(1, ChronoUnit.HOURS);
  Node node;

  Supplier<RollupSeries<Statistics>> seriesSupplier;
  StatisticsRestorer<Statistics> statsRestorer;
  Supplier<Statistics> statisticsSupplier;
  RollupSeriesRestorer<Statistics> restorer;

  @BeforeEach
  public void setup() {
    testResourceFactory = new TestResourceFactory();
    TENANT_ID = "test" + System.currentTimeMillis();
    TENANT_ID_2 = "test" + System.currentTimeMillis();
    node =
        new Node(
            "test-node-" + UUID.randomUUID().toString(),
            "localhost",
            NodeState.METRICS_CONSUMPTION_START);
    statsRestorer= new RolledupStatsRestorer();
    statisticsSupplier = new KllStatSupplier();
    restorer = new RolledUpSeriesRestorer(statsRestorer, statisticsSupplier);
    seriesSupplier = () -> new RollupSeries<>(statsRestorer, statisticsSupplier);
  }

  @Test
  public void testHourlyWithOneSeries() throws Exception {
    var checkpointWriter = testResourceFactory.hourlyCheckpointWriter(node);
    var series = seriesSupplier.get();
    series.writeBatch(
        new MetricsContext("ctx"),
        tenantize("series"),
        new long[] {1000L, 2000L, 3000L},
        new float[] {1.0f, 2.0f, 3.0f});
    var fp = Files.createTempFile("test", "ckpt");
    checkpointWriter.writeCheckpoint(TENANT_ID, series, fp, 0);
    assertTrue(Files.size(fp) > 0);

    var scanner = new HourlyCheckpointScanner();
    var fileRange = new MmapBrs(fp.toFile());
    var metrics = scanner.listMetrics(fileRange);
    assertTrue(metrics.contains(tenantize("series")));
    var md = scanner.getMd(fileRange);
    assertTrue(md.containsKey(tenantize("series")));
    var secondly = scanner.secondly(fileRange, tenantize("series"), md);
    assertEquals(3, secondly.getTs().size());
    assertEquals(Arrays.asList(1, 2, 3), secondly.getTs());
    assertTrue(
        OkapiTestUtils.bytesAreEqual(
            series.getSerializedStats(RollupSeries.secondlyShard(tenantize("series"), 1000)),
            secondly.getVals().get(0).serialize()));
    assertTrue(
        OkapiTestUtils.bytesAreEqual(
            series.getSerializedStats(RollupSeries.secondlyShard(tenantize("series"), 2000)),
            secondly.getVals().get(1).serialize()));
    assertTrue(
        OkapiTestUtils.bytesAreEqual(
            series.getSerializedStats(RollupSeries.secondlyShard(tenantize("series"), 3000)),
            secondly.getVals().get(2).serialize()));

    var minutely = scanner.minutely(fileRange, tenantize("series"), md);
    assertEquals(1, minutely.getTs().size());
    var expectedMinVal =
        series.getSerializedStats(RollupSeries.minutelyShard(tenantize("series"), 1000));
    var gotMinVal = minutely.getVals().get(0).serialize();
    assertTrue(OkapiTestUtils.bytesAreEqual(expectedMinVal, gotMinVal));

    var hourly = scanner.hourly(fileRange, tenantize("series"), md);
    var expectedHourlyVal =
        series.getSerializedStats(RollupSeries.hourlyShard(tenantize("series"), 1000));
    var gotHrVal = hourly.serialize();
    assertTrue(OkapiTestUtils.bytesAreEqual(expectedHourlyVal, gotHrVal));
  }

  @ParameterizedTest
  @MethodSource("testHourlyWithMultipleSeriesSource")
  public void testHourlyWithMultipleSeries(
      Clock clock, List<String> names, List<long[]> ts, List<float[]> vals, long hr)
      throws Exception {
    var checkpointWriter = testResourceFactory.hourlyCheckpointWriter(node);
    int n = names.size();
    var series = seriesSupplier.get();
    for (int i = 0; i < n; i++) {
      series.writeBatch(new MetricsContext("ctx"), tenantize(names.get(i)), ts.get(i), vals.get(i));
    }

    var fp = Files.createTempFile("hourly", ".ckpt");
    checkpointWriter.writeCheckpoint(TENANT_ID, series, fp, hr);
    assertTrue(Files.size(fp) > 0);
    var scanner = new HourlyCheckpointScanner();
    var fileRange = new MmapBrs(fp.toFile());
    var metrics = scanner.listMetrics(fileRange);
    var md = scanner.getMd(fileRange);

    var start = hr * 1000 * 3600L;
    for (int i = 0; i < n; i++) {
      var name = tenantize(names.get(i));
      assertTrue(metrics.contains(name));
      assertTrue(md.containsKey(name));
      {
        var secondly = scanner.secondly(fileRange, name, md);
        var expectedSecondly = minSub(ts.get(i), 1000);
        assertEquals(expectedSecondly, secondly.getTs());
        for (int j = 0; j < expectedSecondly.size(); j++) {
          var atMillis = start + expectedSecondly.get(j) * 1000;
          var shard = RollupSeries.secondlyShard(name, atMillis);
          var val = series.getSerializedStats(shard);
          var got = secondly.getVals().get(j).serialize();
          assertTrue(OkapiTestUtils.bytesAreEqual(val, got));
        }
      }
      {
        var minutely = scanner.minutely(fileRange, name, md);
        var expectedMinutely = minSub(ts.get(i), 1000 * 60);
        assertEquals(expectedMinutely, minutely.getTs());
        for (int j = 0; j < expectedMinutely.size(); j++) {
          var atMillis = start + expectedMinutely.get(j) * 1000 * 60;
          var shard = RollupSeries.minutelyShard(name, atMillis);
          var val = series.getSerializedStats(shard);
          var got = minutely.getVals().get(j).serialize();
          assertTrue(OkapiTestUtils.bytesAreEqual(val, got));
        }
      }
      {
        var hourly = scanner.hourly(fileRange, name, md);
        var key = RollupSeries.hourlyShard(name, start);
        var expectedHourly = series.getSerializedStats(key);
        assertTrue(OkapiTestUtils.bytesAreEqual(expectedHourly, hourly.serialize()));
      }
    }
  }

  public void setup_withSingleShard(Duration writeStart) throws OutsideWindowException {
    var shardMap = testResourceFactory.shardMap(node);
    testResourceFactory.clock(node).setTime(now - writeStart.toMillis());
    var st = testResourceFactory.clock(node).currentTimeMillis();
    shardMap
        .get(0)
        .writeBatch(
            new MetricsContext("ctx"),
            tenantize("series-1"),
            new long[] {
              st + oneMin.toMillis(),
              st + 2 * oneMin.toMillis(),
              st + oneHr.toMillis(),
              st + 2 * oneHr.toMillis(),
            },
            new float[] {1.0f, 2.0f, 3.0f, 4.0f});
    shardMap
        .get(1)
        .writeBatch(
            new MetricsContext("ctx"),
            tenantize("series-1-neg"),
            new long[] {
              st + oneMin.toMillis(),
              st + 2 * oneMin.toMillis(),
              st + oneHr.toMillis(),
              st + 2 * oneHr.toMillis(),
            },
            new float[] {-1.0f, -2.0f, -3.0f, -4.0f});
    shardMap
        .get(0)
        .writeBatch(
            new MetricsContext("ctx"),
            tenantize("series-2"),
            new long[] {
              st + 3 * oneMin.toMillis(),
              st + 4 * oneMin.toMillis(),
              st + oneHr.toMillis(),
              st + 2 * oneHr.toMillis(),
            },
            new float[] {11.0f, 12.0f, 13.0f, 14.0f});
  }

  @Test
  public void testHourlyUpload_withoutPrevious() throws Exception {
    var admissionWindowHrs = 6;
    var admissionWindow = Duration.of(admissionWindowHrs, ChronoUnit.HOURS);
    setup_withSingleShard(admissionWindow);
    // move the clock forward to today
    testResourceFactory.clock(node).setTime(now);
    testResourceFactory.setAdmissionWindowHrs(6);
    var shardMap = testResourceFactory.shardMap(node);
    var statsA =
        shardMap
            .get(0)
            .getStats(
                RollupSeries.secondlyShard(
                    tenantize("series-1"), now + oneMin.toMillis() - admissionWindow.toMillis()));

    //
    var checkpointWriter = testResourceFactory.hourlyCheckpointWriter(node);
    printBytes(statsA.get().serialize());
    // uploading checkpoint
    checkpointWriter.uploadHourlyCheckpoint();
    printBytes(statsA.get().serialize());

    //
    var s3Client = testResourceFactory.s3Client();
    var timeOnNode = testResourceFactory.clock(node).getTime();
    var hr = (timeOnNode) / 1000 / 3600 - admissionWindowHrs;
    var prefix = S3Prefixes.hourlyPrefix(TENANT_ID, hr, 0);
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
    assertEquals(Set.of(tenantize("series-1"), tenantize("series-2")), list);
    var md = hourlyScanner.getMd(brs);

    // check secondly data
    var secondly = hourlyScanner.secondly(brs, tenantize("series-1"), md);
    assertEquals(2, secondly.getTs().size());
    assertEquals(Arrays.asList(60, 120), secondly.getTs());
    var expectedSecondly1 =
        testResourceFactory
            .shardMap(node)
            .get(0)
            .getSerializedStats(
                RollupSeries.secondlyShard(
                    tenantize("series-1"), now + oneMin.toMillis() - admissionWindow.toMillis()));
    var expectedSecondly2 =
        testResourceFactory
            .shardMap(node)
            .get(0)
            .getSerializedStats(
                RollupSeries.secondlyShard(
                    tenantize("series-1"),
                    now + 2 * oneMin.toMillis() - admissionWindow.toMillis()));
    assertTrue(
        OkapiTestUtils.bytesAreEqual(expectedSecondly1, secondly.getVals().get(0).serialize()));
    assertTrue(
        OkapiTestUtils.bytesAreEqual(expectedSecondly2, secondly.getVals().get(1).serialize()));

    // check minutely stats
    var minutely = hourlyScanner.minutely(brs, tenantize("series-1"), md);
    assertEquals(2, minutely.getTs().size());
    assertEquals(Arrays.asList(1, 2), minutely.getTs());
    var expectedMinutely1 =
        testResourceFactory
            .shardMap(node)
            .get(0)
            .getSerializedStats(
                RollupSeries.minutelyShard(
                    tenantize("series-1"), now + oneMin.toMillis() - admissionWindow.toMillis()));
    var expectedMinutely2 =
        testResourceFactory
            .shardMap(node)
            .get(0)
            .getSerializedStats(
                RollupSeries.minutelyShard(
                    tenantize("series-1"),
                    now + 2 * oneMin.toMillis() - admissionWindow.toMillis()));
    assertTrue(
        OkapiTestUtils.bytesAreEqual(expectedMinutely1, minutely.getVals().get(0).serialize()));
    assertTrue(
        OkapiTestUtils.bytesAreEqual(expectedMinutely2, minutely.getVals().get(1).serialize()));

    // check hourly stats
    var expectedHourly =
        testResourceFactory
            .shardMap(node)
            .get(0)
            .getSerializedStats(
                RollupSeries.hourlyShard(tenantize("series-1"), now - admissionWindow.toMillis()));
    var hourly = hourlyScanner.hourly(brs, tenantize("series-1"), md);
    assertTrue(OkapiTestUtils.bytesAreEqual(expectedHourly, hourly.serialize()));

    // check that ZK state was updated
    var zkState =
        testResourceFactory.fleetMetadata().getData(ZkPaths.lastCheckpointedHour(node.id()));
    assertNotNull(zkState);
    var expectedPrev = (now - admissionWindow.toMillis()) / 1000L / 3600;
    assertEquals(expectedPrev, Longs.fromByteArray(zkState));
  }

  @Test
  public void testHourlyUpload_withPrevious() throws Exception {
    setup_withSingleShard(Duration.of(25, ChronoUnit.HOURS));
    // move the clock forward to today
    testResourceFactory.clock(node).setTime(now + oneHr.toMillis());
    testResourceFactory.setAdmissionWindowHrs(24);
    var frozenWriter = testResourceFactory.hourlyCheckpointWriter(node);
    var expectedCheckpointPath = Files.createTempFile("expected", "ckpt");
    var hr = (now + oneHr.toMillis() - 24 * oneHr.toMillis()) / 1000L / 3600;
    testResourceFactory
        .hourlyCheckpointWriter(node)
        .writeCheckpoint(
            TENANT_ID, testResourceFactory.shardMap(node).get(0), expectedCheckpointPath, hr);
    frozenWriter.uploadHourlyCheckpoint();
    var s3 = testResourceFactory.s3Client();
    var prefix = S3Prefixes.hourlyPrefix(TENANT_ID, hr, 0);
    var bytes =
        s3.getObject(
                GetObjectRequest.builder()
                    .bucket(testResourceFactory.getDataBucket())
                    .key(prefix)
                    .build())
            .readAllBytes();
    assertTrue(bytes.length > 0);

    assertTrue(OkapiTestUtils.bytesAreEqual(bytes, Files.readAllBytes(expectedCheckpointPath)));
    var lastHr = testResourceFactory.nodeStateRegistry(node).getLastCheckpointedHour();
    assertEquals(hr, lastHr.get());
  }

  @Test
  public void testHourly_withSkipped() throws Exception {
    setup_withSingleShard(Duration.of(24, ChronoUnit.HOURS));
    testResourceFactory.setAdmissionWindowHrs(24);
    testResourceFactory.clock(node).setTime(now + 3 * oneHr.toMillis());
    var toWrite = (now - 24 * oneHr.toMillis()) / 1000 / 3600;
    testResourceFactory.nodeStateRegistry(node).updateLastCheckPointedHour(toWrite - 1);
    // should write a checkpoint for hr: now - 24Hrs, instead of now - 24hrs + 3hrs
    testResourceFactory.hourlyCheckpointWriter(node).uploadHourlyCheckpoint();
    var shardMap = testResourceFactory.shardMap(node);
    var writer = testResourceFactory.hourlyCheckpointWriter(node);
    var temp1 = Files.createTempFile("temp1", ".tmp");
    writer.writeCheckpoint(TENANT_ID, shardMap.get(0), temp1, toWrite);
    var temp2 = Files.createTempFile("temp1", ".tmp");
    writer.writeCheckpoint(TENANT_ID, shardMap.get(0), temp2, toWrite);
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
        .writeCheckpoint(
            TENANT_ID, testResourceFactory.shardMap(node).get(0), expectedCheckpointPath, toWrite);
    assertTrue(OkapiTestUtils.bytesAreEqual(bytes, Files.readAllBytes(expectedCheckpointPath)));
    var lastHr = testResourceFactory.nodeStateRegistry(node).getLastCheckpointedHour();
    assertEquals(toWrite, lastHr.get());
  }

  @Test
  public void testHourly_withSkipped_debugging() throws Exception {
    setup_withSingleShard(Duration.of(24, ChronoUnit.HOURS));
    testResourceFactory.setAdmissionWindowHrs(24);
    testResourceFactory.clock(node).setTime(now + 3 * oneHr.toMillis());
    var toWrite = (now - 24 * oneHr.toMillis()) / 1000 / 3600;
    testResourceFactory.nodeStateRegistry(node).updateLastCheckPointedHour(toWrite - 1);
    // should write a checkpoint for hr: now - 24Hrs, instead of now - 24hrs + 3hrs
    testResourceFactory.hourlyCheckpointWriter(node).uploadHourlyCheckpoint();
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
        .writeCheckpoint(
            TENANT_ID, testResourceFactory.shardMap(node).get(0), expectedCheckpointPath, toWrite);
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
    var clock = testResourceFactory.clock(node);
    // rewind the clock
    clock.setTime(now - Duration.of(admissionWindowHrs, ChronoUnit.HOURS).toMillis());
    var t = clock.currentTimeMillis();
    shardMap
        .get(0)
        .writeBatch(
            MetricsContext.createContext("test"),
            tenantize(TENANT_ID, "series-1"),
            new long[] {t, t + 100, t + 200},
            new float[] {1.0f, 2.0f, 3.0f});

    // move the clock forward
    clock.setTime(now);
    // do the upload
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
  public void testParquetUpload_single_shard_multiple_tenants() throws Exception {
    // cases : single shard, multiple shards, multiple tenants.
    var admissionWindowHrs = 6;
    testResourceFactory.setAdmissionWindowHrs(admissionWindowHrs);
    var shardMap = testResourceFactory.shardMap(node);
    var clock = testResourceFactory.clock(node);
    // rewind the clock
    clock.setTime(now - Duration.of(admissionWindowHrs, ChronoUnit.HOURS).toMillis());
    var t = clock.currentTimeMillis();
    shardMap
        .get(0)
        .writeBatch(
            MetricsContext.createContext("test"),
            tenantize(TENANT_ID, "series-1"),
            new long[] {t, t + 100, t + 200},
            new float[] {1.0f, 2.0f, 3.0f});

    shardMap
        .get(1)
        .writeBatch(
            MetricsContext.createContext("test"),
            tenantize(TENANT_ID_2, "series-1"),
            new long[] {t, t + 100, t + 200},
            new float[] {1.0f, 2.0f, 3.0f});
    // move the clock forward
    clock.setTime(now);
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
    var q = 1000 * 3600;
    var t0 = (System.currentTimeMillis()) / q + 1234;
    return Stream.of(
        Arguments.of(
            new FakeClock(10),
            Arrays.asList("series"),
            Arrays.asList(new long[] {1000, 2000, 3000}),
            Arrays.asList(new float[] {1.f, 2.f, 3.f}),
            0L),
        Arguments.of(
            new FakeClock(10),
            Arrays.asList("series", "series-2"),
            Arrays.asList(new long[] {1000, 2000, 3000}, new long[] {1000, 2000, 3000}),
            Arrays.asList(new float[] {1.f, 2.f, 3.f}, new float[] {11.f, 12.f, 13.f}),
            0L),
        Arguments.of(
            new FakeClock(t0),
            Arrays.asList("series", "series-2"),
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
            t0 / 1000 / 3600),
        Arguments.of(
            new FakeClock(t0),
            Arrays.asList("series", "series-2"),
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
            t0 / 1000 / 3600));
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
