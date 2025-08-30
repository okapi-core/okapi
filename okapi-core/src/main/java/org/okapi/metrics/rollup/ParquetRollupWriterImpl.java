package org.okapi.metrics.rollup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.okapi.Statistics;
import org.okapi.metrics.PathRegistry;
import org.okapi.metrics.avro.AggregationType;
import org.okapi.metrics.avro.MetricRow;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.MetricsPathParser;
import org.okapi.metrics.paths.PathSet;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.stats.ReadonlyRestorer;
import org.okapi.metrics.stats.UpdatableStatistics;

@AllArgsConstructor
public class ParquetRollupWriterImpl<T extends UpdatableStatistics> implements ParquetRollupWriter<T> {
  PathRegistry pathRegistry;
  PathSet pathSet;
  RocksStore rocksStore;

  private org.apache.hadoop.fs.Path open(Path parquetPath) throws IOException {
    Files.createDirectories(parquetPath.getParent());
    Files.deleteIfExists(parquetPath);
    return new org.apache.hadoop.fs.Path(parquetPath.toAbsolutePath().toString());
  }

  @Override
  public void writeDump(String tenantId, long hr) throws IOException {
    var parquetPath = pathRegistry.parquetPath(hr, tenantId);
    var hPath = open(parquetPath);
    try (ParquetWriter<GenericRecord> writer =
        AvroParquetWriter.<GenericRecord>builder(hPath)
            .withSchema(MetricRow.getClassSchema())
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withConf(new Configuration(false))
            .build(); ) {
      var metricPaths = pathSet.list();
      var eligiblePaths =
          metricPaths.values().stream()
              .flatMap(Set::stream)
              .map(MetricsPathParser::parse)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .filter(path -> path.tenantId().equals(tenantId))
              .toList();
      for (var path : eligiblePaths) {
        var serializedPath = MetricPaths.convertToPath(path.tenantId(), path.name(), path.tags());
        var shards = pathSet.shardsForPath(serializedPath);
        var reader =
            FirstMatchReader.getFirstMatchReader(
                pathRegistry, rocksStore, ReadonlyRestorer::new, shards);
        // effects
        var hourlyStat = reader.hourlyStats(serializedPath, hr);
        if (hourlyStat.isEmpty()) {
          // this metric doesn't exist or there will be an hourly stat
          continue;
        }

        // aggregate
        {
          var secondStart = hr * 3600L;
          for (int i = 0; i < 3600; i++) {
            var roundedSecond = (secondStart + i) * 1000L;
            var stat = reader.secondlyStats(serializedPath, roundedSecond);
            if (stat.isEmpty()) continue;
            var row =
                toRow(
                    roundedSecond, path.name(), path.tags(), stat.get(), AggregationType.SECONDLY);
            writer.write(row);
          }
        }
        {
          var minuteStart = hr * 60L;
          for (int i = 0; i < 60; i++) {
            var roundedMinute = (minuteStart + i) * 60 * 1000L;
            var stat = reader.secondlyStats(serializedPath, roundedMinute);
            if (stat.isEmpty()) continue;
            var row =
                toRow(
                    roundedMinute, path.name(), path.tags(), stat.get(), AggregationType.MINUTELY);
            writer.write(row);
          }
          {
            var roundedHr = hr * 3600 * 1000L;
            var stat = reader.hourlyStats(serializedPath, roundedHr);
            if (stat.isEmpty()) continue;
            var row =
                toRow(roundedHr, path.name(), path.tags(), stat.get(), AggregationType.HOURLY);
            writer.write(row);
          }
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

  private static <T extends Statistics> MetricRow toRow(
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
}
