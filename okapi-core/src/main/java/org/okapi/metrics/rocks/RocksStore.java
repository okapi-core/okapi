package org.okapi.metrics.rocks;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class RocksStore implements Closeable {

  Map<Path, RocksDB> dbCache;
  ReadWriteLock readWriteLock;

  public RocksStore() throws IOException {
    this.readWriteLock = new ReentrantReadWriteLock();
    this.dbCache = new HashMap<>();
  }

  private Optional<RocksDB> lazyOpen(Path path, boolean create) throws IOException {
    if (!Files.exists(path) && !create) {
      return Optional.empty();
    }
    if (!Files.exists(path) && create) {
      Files.createDirectories(path);
    }
    if (dbCache.containsKey(path)) {
      return Optional.ofNullable(dbCache.get(path));
    }
    this.readWriteLock.writeLock().lock();
    try {
      if (dbCache.containsKey(path)) {
        return Optional.ofNullable(dbCache.get(path));
      }
      final RocksDB db = RocksDB.open(path.toString());
      dbCache.put(path, db);
      return Optional.of(db);
    } catch (RocksDBException e) {
      throw new RuntimeException(e);
    } finally {
      this.readWriteLock.writeLock().unlock();
    }
  }

  public RocksDbWriter rocksWriter(Path path) throws IOException {
    var rocks = lazyOpen(path, true);
    return new RocksDbWriter(rocks.get());
  }

  public Optional<RocksDbReader> rocksReader(Path path) throws IOException {
    var rocks = lazyOpen(path, false);
    return rocks.map(RocksDbReader::new);
  }

  @Override
  public void close() throws IOException {
    for (var rocks : this.dbCache.values()) {
      rocks.close();
    }
  }
}
