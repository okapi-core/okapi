package org.okapi.metrics.rollup;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.okapi.metrics.avro.AggregationType;
import org.okapi.metrics.avro.MetricRow;
import org.okapi.metrics.common.MetricsPathParser;
import org.okapi.metrics.stats.Statistics;

public class ParquetRollupWriterImpl<T extends  Statistics> implements ParquetRollupWriter<T> {
  private boolean opened = false;
  private ParquetWriter<GenericRecord> writer;
  private String tenantId;
  private Path path;

  @Override
  public void open(String tenantId, Path parquetPath) throws IOException {
    this.tenantId = tenantId;
    this.path = parquetPath;
    if (opened) throw new IllegalStateException("Exporter already opened");

    // Ensure parent directory exists and file is fresh (avoid FileAlreadyExistsException)
    Files.createDirectories(parquetPath.getParent());
    Files.deleteIfExists(parquetPath);

    // Build Hadoop Path (avoid shadowing)
    org.apache.hadoop.fs.Path hPath =
        new org.apache.hadoop.fs.Path(parquetPath.toAbsolutePath().toString());

    writer =
        AvroParquetWriter.<GenericRecord>builder(hPath)
            .withSchema(MetricRow.getClassSchema())
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withConf(new Configuration(false))
            .build();

    opened = true;
  }

  @Override
  public void consume(RollupSeries<T> series, long hr) throws IOException {
    var metricPaths = series.listMetricPaths();
    var startTime = hr * 3600 * 1000L;
    for (var path : metricPaths) {
      var tenantId = MetricsPathParser.tenantId(path);
      var parsed = MetricsPathParser.parse(path);
      if (parsed.isEmpty()) continue;
      if (tenantId.isEmpty() || !tenantId.get().equals(this.tenantId)) continue;
      var hourlyKey =
          HashFns.hourlyBucket(
              path, startTime); // take the start time and increase by a second to avoid boundary
      // effects
      var hourlyStat = series.getStats(hourlyKey);
      if (hourlyStat.isEmpty()) {
        // this metric doesn't exist or there will be an hourly stat
        continue;
      }

      // aggregate
      {
        var secondStart = hr * 3600L;
        for (int i = 0; i < 3600; i++) {
          var key = HashFns.secondlyBucket(path, 1000L * (secondStart + i));
          var stat = series.getStats(key);
          if (stat.isEmpty()) continue;
          var quantizedSecond = (secondStart + i) * 1000L;
          var row =
              toRow(
                  quantizedSecond,
                  parsed.get().name(),
                  parsed.get().tags(),
                  stat.get(),
                  AggregationType.SECONDLY);
          writer.write(row);
        }
      }
      {
        var minuteStart = hr * 60L;
        for (int i = 0; i < 60; i++) {
          var key = HashFns.minutelyBucket(path, 1000L * 60 * (minuteStart + i));
          var stat = series.getStats(key);
          if (stat.isEmpty()) continue;
          var quantizedMinute = (minuteStart + i) * 60 * 1000L;
          var row =
              toRow(
                  quantizedMinute,
                  parsed.get().name(),
                  parsed.get().tags(),
                  stat.get(),
                  AggregationType.MINUTELY);
          writer.write(row);
        }
        {
          var quantizedHr = hr * 3600 * 1000L;
          var key = HashFns.hourlyBucket(path, quantizedHr);
          var stat = series.getStats(key);
          if (stat.isEmpty()) continue;
          var row =
              toRow(
                  quantizedHr,
                  parsed.get().name(),
                  parsed.get().tags(),
                  stat.get(),
                  AggregationType.HOURLY);
          writer.write(row);
        }
      }
    }
  }

  private static Map<CharSequence, CharSequence> toAvroTags(Map<String, String> tags) {
    if (tags == null) return Collections.emptyMap();
    return tags.entrySet().stream()
        .collect(
            Collectors.toMap(e -> (CharSequence) e.getKey(), e -> (CharSequence) e.getValue()));
  }

  private static <T extends  Statistics> MetricRow toRow(
      long quantizedTime,
      String name,
      Map<String, String> tags,
      T rolledUpStatistics2,
      AggregationType aggregationType) {
    var rowBuilder =
        MetricRow.newBuilder()
            .setMetric(name)
            .setTags(toAvroTags(tags))
            .setSum(rolledUpStatistics2.getSum())
            .setCount((int) rolledUpStatistics2.getCount())
            .setMin(rolledUpStatistics2.min())
            .setMax(rolledUpStatistics2.max())
            .setP25(rolledUpStatistics2.percentile(0.25))
            .setP50(rolledUpStatistics2.percentile(0.50))
            .setP75(rolledUpStatistics2.percentile(0.75))
            .setP90(rolledUpStatistics2.percentile(0.90))
            .setP99(rolledUpStatistics2.percentile(0.99))
            .setStddev(computeStddev(rolledUpStatistics2));

    rowBuilder.setTimestamp(quantizedTime);
    rowBuilder.setAggType(aggregationType);
    return rowBuilder.build();
  }

  private static float computeStddev(Statistics st) {
    var count = st.getCount();
    if (count <= 1f) return 0f;
    var variance = st.getSumOfDeviationsSquared();
    return (float) Math.sqrt(Math.max(variance, 0d));
  }

  @Override
  public void close() throws IOException {
    try (FileChannel ch =
        FileChannel.open(Paths.get(this.path.toString()), StandardOpenOption.WRITE)) {
      ch.force(true);
    }
    writer.close();
  }
}
