package org.okapi.metrics.rollup;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import lombok.AllArgsConstructor;
import org.okapi.clock.Clock;
import org.okapi.metrics.CheckpointUploaderDownloader;
import org.okapi.metrics.NodeStateRegistry;
import org.okapi.metrics.PathRegistry;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.common.MetricsPathParser;
import org.okapi.metrics.io.OkapiIo;
import org.okapi.metrics.stats.Statistics;

@AllArgsConstructor
public class FrozenMetricsUploader {

  ShardMap shardMap;
  CheckpointUploaderDownloader checkpointUploaderDownloader;
  PathRegistry pathRegistry;
  NodeStateRegistry nodeStateRegistry;
  Clock clock;
  long admissionWindowHrs;

  public void writeCheckpoint(String forTenant, RollupSeries series, Path fp, long hr)
      throws IOException {
    var metricPaths = series.listMetricPaths();
    var startTime = hr * 3600 * 1000L;
    var off = 0L;
    Map<String, Long[]> pathToOffset = new HashMap<>();

    try (var fos = new FileOutputStream(fp.toFile())) {
      for (var metricPath : metricPaths) {
        var tenantId = MetricsPathParser.tenantId(metricPath);
        if (tenantId.isEmpty() || !tenantId.get().equals(forTenant)) continue;
        var hourlyKey =
            RollupSeries.hourlyShard(
                metricPath,
                startTime); // take the start time and increase by a second to avoid boundary
        // effects
        var hourlyStat = series.getStats(hourlyKey);
        if (hourlyStat.isEmpty()) {
          // this metric doesn't exist or there will be an hourly stat
          continue;
        }

        var secondlyOffset = off;
        // write secondly data
        {
          var secondStart = hr * 3600L;
          List<Integer> seconds = new ArrayList<>();
          List<Statistics> statistics = new ArrayList<>();
          for (int i = 0; i < 3600; i++) {
            var key = RollupSeries.secondlyShard(metricPath, 1000L * (secondStart + i));
            var stats = series.getStats(key);
            if (stats.isEmpty()) {
              continue;
            }
            seconds.add(i);
            statistics.add(stats.get());
          }

          off += OkapiIo.writeInt(fos, seconds.size());
          for (int i = 0; i < seconds.size(); i++) {
            off += OkapiIo.writeInt(fos, seconds.get(i));
            off += OkapiIo.writeBytes(fos, statistics.get(i).serialize());
          }
        }

        // write minutely data
        var minutelyOffset = off;
        {
          var minuteStart = 60L * hr;
          List<Integer> mins = new ArrayList<>();
          List<Statistics> minStats = new ArrayList<>();
          for (int i = 0; i < 60; i++) {
            var key = RollupSeries.minutelyShard(metricPath, 60 * 1000L * (minuteStart + i));
            var stats = series.getStats(key);
            if (stats.isEmpty()) {
              continue;
            }
            mins.add(i);
            minStats.add(stats.get());
          }

          off += OkapiIo.writeInt(fos, mins.size());
          for (int i = 0; i < mins.size(); i++) {
            off += OkapiIo.writeInt(fos, mins.get(i));
            off += OkapiIo.writeBytes(fos, minStats.get(i).serialize());
          }
        }

        // write hourly data
        var hourlyOffset = off;
        {
          var key = RollupSeries.hourlyShard(metricPath, 3600 * 1000L * (hr));
          var stats = series.getStats(key).get();
          off += OkapiIo.writeBytes(fos, stats.serialize());
        }

        var offsets = new Long[] {secondlyOffset, minutelyOffset, hourlyOffset, off};
        pathToOffset.put(metricPath, offsets);
      }

      if (pathToOffset.isEmpty()) {
        // Nothing to write.
        return;
      }

      var metadataOffset = off;
      {
        off += OkapiIo.writeInt(fos, metricPaths.size());
        for (var p : metricPaths) {
          off += OkapiIo.writeString(fos, p);
          for (var l : pathToOffset.get(p)) {
            off += OkapiIo.writeLong(fos, l);
          }
        }
      }
      off += OkapiIo.writeLong(fos, metadataOffset);
      fos.getChannel().force(true);
    }
  }

  public void uploadHourlyCheckpoint(long hr) throws Exception {
    var tenants = new HashSet<String>();

    for (var shard : shardMap.shards()) {
      var series = shardMap.get(shard);
      var keys = series.getKeys();
      for (var k : keys) {
        var optionalTenantId = MetricsPathParser.tenantId(k);
        optionalTenantId.ifPresent(tenants::add);
      }
    }

    for (var tenant : tenants) {
      var parquetWriter = new ParquetRollupWriterImpl();
      var parquetPath = pathRegistry.parquetPath(hr, tenant);
      parquetWriter.open(tenant, parquetPath);
      for (var shard : shardMap.shards()) {
        var fp = pathRegistry.checkpointUploaderRoot(shard, hr, tenant);
        var series = shardMap.get(shard);
        writeCheckpoint(tenant, series, fp, hr);
        checkpointUploaderDownloader.uploadHourlyCheckpoint(tenant, fp, hr, shard);
        // write the parquet dump for all frozen metric paths
        parquetWriter.consume(series, hr);
      }

      parquetWriter.close();
      checkpointUploaderDownloader.uploadParquetDump(tenant, parquetPath, hr);
    }

    nodeStateRegistry.updateLastCheckPointedHour(hr);
  }

  public void uploadHourlyCheckpoint() throws Exception {
    var lastCheckpointed = nodeStateRegistry.getLastCheckpointedHour();
    var targetHour = clock.currentTimeMillis() / 1000 / 3600 - admissionWindowHrs;
    if (lastCheckpointed.isPresent()) {
      targetHour = Math.min(1 + lastCheckpointed.get(), targetHour);
    }
    uploadHourlyCheckpoint(targetHour);
  }
}
