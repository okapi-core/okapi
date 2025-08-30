package org.okapi.metrics.rollup;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.Statistics;
import org.okapi.clock.Clock;
import org.okapi.metrics.CheckpointUploaderDownloader;
import org.okapi.metrics.NodeStateRegistry;
import org.okapi.metrics.PathRegistry;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.MetricsPathParser;
import org.okapi.metrics.io.OkapiIo;
import org.okapi.metrics.paths.PathSet;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.stats.ReadonlyRestorer;
import org.okapi.metrics.stats.UpdatableStatistics;

@Slf4j
@AllArgsConstructor
public class FrozenMetricsUploader {

  CheckpointUploaderDownloader checkpointUploaderDownloader;
  PathRegistry pathRegistry;
  NodeStateRegistry nodeStateRegistry;
  Clock clock;
  long admissionWindowHrs;
  PathSet pathSet;
  RocksStore rocksStore;
  ParquetRollupWriter<UpdatableStatistics> parquetWriter;

  public void writeCheckpoint(String tenant, Path fp, long hr) throws IOException {
    var metricPaths = pathSet.list();
    var eligiblePaths =
        metricPaths.values().stream()
            .flatMap(Set::stream)
            .map(MetricsPathParser::parse)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(path -> path.tenantId().equals(tenant))
            .toList();
    var off = 0L;
    Map<String, Long[]> pathToOffset = new HashMap<>();

    var startTime = hr * 3600 * 1000L;
    try (var fos = new FileOutputStream(fp.toFile())) {
      for (var path : eligiblePaths) {
        var serializedPath = MetricPaths.convertToPath(path.tenantId(), path.name(), path.tags());
        var shards = pathSet.shardsForPath(serializedPath);
        if (shards.isEmpty()) continue;
        var reader =
            FirstMatchReader.getFirstMatchReader(
                pathRegistry, rocksStore, ReadonlyRestorer::new, shards);
        var hourlyStat = reader.hourlyStats(serializedPath, startTime);
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
            var roundedSecond = (1000L) * (secondStart + i);
            var stats = reader.secondlyStats(serializedPath, roundedSecond);
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
            var roundedMinute = 60 * 1000L * (minuteStart + i);
            var stats = reader.minutelyStats(serializedPath, roundedMinute);
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
          var roundedHr = 3600 * 1000L * (hr);
          var stats = reader.hourlyStats(serializedPath, roundedHr);
          off += OkapiIo.writeBytes(fos, stats.get().serialize());
        }

        var offsets = new Long[] {secondlyOffset, minutelyOffset, hourlyOffset, off};
        pathToOffset.put(serializedPath, offsets);
      }

      if (pathToOffset.isEmpty()) {
        // Nothing to write.
        return;
      }

      var metadataOffset = off;
      {
        off += OkapiIo.writeInt(fos, pathToOffset.size());
        for (var p : pathToOffset.keySet()) {
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
    var tenants =
        pathSet.list().values().stream()
            .flatMap(Set::stream)
            .map(MetricsPathParser::parse)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(MetricsPathParser.MetricsRecord::tenantId)
            .toList();

    for (var tenant : tenants) {
      var parquetPath = pathRegistry.parquetPath(hr, tenant);
      var fp = pathRegistry.hourlyCheckpointPath(hr, tenant);
      // upload okapi checkpoint
      writeCheckpoint(tenant, fp, hr);
      checkpointUploaderDownloader.uploadHourlyCheckpoint(tenant, fp, hr);

      // upload parquet checkpoint
      parquetWriter.writeDump(tenant, hr);
      checkpointUploaderDownloader.uploadParquetDump(tenant, parquetPath, hr);
    }
    nodeStateRegistry.updateLastCheckPointedHour(hr);
  }

  public void uploadHourlyCheckpoint() throws Exception {
    var lastCheckpointed = nodeStateRegistry.getLastCheckpointedHour();
    var targetHour = clock.currentTimeMillis() / 1000 / 3600 - admissionWindowHrs;
    var alreadyCheckpointed = lastCheckpointed.isPresent() && lastCheckpointed.get() == targetHour;
    if(alreadyCheckpointed){
      log.info("Nothing do to.");
    }
    if (lastCheckpointed.isPresent()) {
      targetHour = Math.min(1 + lastCheckpointed.get(), targetHour);
    }
    uploadHourlyCheckpoint(targetHour);
  }
}
