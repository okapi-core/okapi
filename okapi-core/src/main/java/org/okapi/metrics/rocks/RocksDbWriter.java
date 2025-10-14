package org.okapi.metrics.rocks;

import lombok.AllArgsConstructor;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

@AllArgsConstructor
public class RocksDbWriter {

  RocksDB rocksDB;

  public void put(byte[] key, byte[] value) throws RocksDBException {
    rocksDB.put(key, value);
  }

  public byte[] get(byte[] key) throws RocksDBException {
    return rocksDB.get(key);
  }

  public void sync() throws RocksDBException {
    rocksDB.flushWal(true);
  }
}
