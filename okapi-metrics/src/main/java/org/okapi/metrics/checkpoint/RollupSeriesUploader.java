package org.okapi.metrics.checkpoint;

import org.okapi.metrics.CheckpointUploaderDownloader;
import org.okapi.metrics.rollup.RollupSeries;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class RollupSeriesUploader {

  RollupSeries series;
  CheckpointUploaderDownloader checkpointUploader;

  public void checkpoint(Path filePath) throws IOException {
    // 24 hr dedup string for checkpointing
    var fileId = System.currentTimeMillis() / 1000 / (2 * 3600);
    var ckptPath = filePath.resolve("checkpoint_" + fileId + "_" + UUID.randomUUID() + ".ckpt");
    throw new IOException("Checkpointing not implemented yet");
  }
}
