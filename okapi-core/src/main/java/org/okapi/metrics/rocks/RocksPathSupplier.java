package org.okapi.metrics.rocks;

import lombok.AllArgsConstructor;

import java.nio.file.Path;
import java.util.function.Function;

@AllArgsConstructor
public class RocksPathSupplier implements Function<Integer, Path> {
  Path rocksRoot;

  @Override
  public Path apply(Integer integer) {
    return rocksRoot.resolve(Integer.toString(integer));
  }
}
