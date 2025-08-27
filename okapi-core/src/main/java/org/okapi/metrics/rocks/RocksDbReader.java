package org.okapi.metrics.rocks;

import lombok.AllArgsConstructor;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.List;

@AllArgsConstructor
public class RocksDbReader {

  RocksDB rocksDB;

  public byte[] get(byte[] key) throws RocksDBException {
    return rocksDB.get(key);
  }

  public List<byte[]> getBatch(List<byte[]> keys) throws RocksDBException {
    return rocksDB.multiGetAsList(keys);
  }
}
