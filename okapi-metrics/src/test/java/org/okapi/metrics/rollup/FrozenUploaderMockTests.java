package org.okapi.metrics.rollup;

import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.okapi.clock.Clock;
import org.okapi.metrics.CheckpointUploaderDownloader;
import org.okapi.metrics.NodeStateRegistry;
import org.okapi.metrics.PathRegistry;
import org.okapi.metrics.paths.PathSet;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.stats.Statistics;

public class FrozenUploaderMockTests {

  FrozenMetricsUploader frozenMetricsUploader;
  CheckpointUploaderDownloader checkpointUploaderDownloader;
  PathRegistry pathRegistry;
  NodeStateRegistry nodeStateRegistry;
  Clock clock;
  long admissionWindowHrs = 6;
  PathSet pathSet;
  RocksStore rocksStore;
  ParquetRollupWriter<Statistics> parquetWriter;

  @BeforeEach
  public void setup() {
    checkpointUploaderDownloader = Mockito.mock(CheckpointUploaderDownloader.class);
    pathRegistry = Mockito.mock(PathRegistry.class);
    nodeStateRegistry = Mockito.mock(NodeStateRegistry.class);
    clock = Mockito.mock(Clock.class);
    pathSet = Mockito.mock(PathSet.class);
    rocksStore = Mockito.mock(RocksStore.class);
    parquetWriter = Mockito.mock(ParquetRollupWriter.class);
    frozenMetricsUploader =
        new FrozenMetricsUploader(
            checkpointUploaderDownloader,
            pathRegistry,
            nodeStateRegistry,
            clock,
            admissionWindowHrs,
            pathSet,
            rocksStore,
            parquetWriter);
  }

  @Test
  public void testFrozenUploaderWithPrevious() throws Exception {
    var lastUploaded = System.currentTimeMillis() / 3600_000 - admissionWindowHrs - 1;
    var target = 1 + lastUploaded;
    var start = 3600_000L * (System.currentTimeMillis() / 3600_000L);
    when(clock.currentTimeMillis()).thenReturn(start);
    Mockito.when(nodeStateRegistry.getLastCheckpointedHour()).thenReturn(Optional.of(lastUploaded));
    Mockito.when(pathSet.list()).thenReturn(Map.of(0, Set.of("tenantA:metric1{}")));
    Mockito.when(pathRegistry.hourlyCheckpointPath(Mockito.eq(6), Mockito.eq("tenantA"))).thenReturn(Path.of("/path/to/file"));
    when(pathRegistry.parquetPath(eq(target), eq("tenantA"))).thenReturn(Path.of("/parquet-path"));
    frozenMetricsUploader.uploadHourlyCheckpoint();
  }
}
