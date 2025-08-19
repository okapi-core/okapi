package org.okapi.metrics.service.runnables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.okapi.clock.Clock;
import org.okapi.exceptions.ExceptionUtils;
import org.okapi.metrics.ShardMap;

@Slf4j
public class Checkpointer implements Runnable {
  ShardMap shardMap;
  ScheduledExecutorService executorService;
  Duration checkpointGap;
  Path snapshotDir;
  Clock clock;

  public Checkpointer(
      ShardMap shardMap,
      ScheduledExecutorService executorService,
      Duration checkpointGap,
      Path snapshotDir,
      Clock clock)
      throws IOException {
    this.shardMap = shardMap;
    this.executorService = executorService;
    this.checkpointGap = checkpointGap;
    this.snapshotDir = snapshotDir;
    if (!Files.exists(snapshotDir)) {
      Files.createDirectories(snapshotDir);
    }
    this.clock = clock;
  }

  public Path getSnapshotPath() {
    return snapshotDir.resolve("shardMap.snapshot");
  }

  public void schedule() {
    executorService.schedule(this, checkpointGap.getSeconds(), TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    try {
      writeSnapshot();
    } catch (Exception e) {
      log.error("Got exception while checkpointing: {}", ExceptionUtils.debugFriendlyMsg(e));
    } finally {
      schedule();
    }
  }

  public void writeSnapshot() throws IOException {
    var fname = "snapshot-" + clock.currentTimeMillis();
    var path = snapshotDir.resolve(fname);
    log.info("Writing checkpoint {}", fname);
    shardMap.snapshot(path);
    Files.move(path, getSnapshotPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    Files.deleteIfExists(path);
    log.info("Writing written {}", getSnapshotPath());
  }
}
