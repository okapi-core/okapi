package org.okapi.metrics.paths;

import java.nio.file.Path;

public class PersistedSetWalPathSupplierImpl implements PersistedSetWalPathSupplier {

  Path root;

  public PersistedSetWalPathSupplierImpl(Path root) {
    this.root = root;
  }

  @Override
  public Path apply(Integer integer) {
    return root.resolve(Integer.toString(integer));
  }
}
