package org.okapi.metrics.persistence;

import java.nio.file.Path;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.WriteBackRequest;
import org.okapi.metrics.stats.UpdatableStatistics;

public class RocksPersistentStore implements PersistentStore {
  Path root;
  SharedMessageBox<WriteBackRequest> requests;

  public RocksPersistentStore(Path root) {
    this.root = root;
  }

  @Override
  public UpdatableStatistics get(String key) {
    throw new RuntimeException();
  }

  @Override
  public void put(String key, UpdatableStatistics statistics) {}
}
