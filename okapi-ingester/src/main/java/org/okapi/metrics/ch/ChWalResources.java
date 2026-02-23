package org.okapi.metrics.ch;

import java.io.IOException;
import java.nio.file.Path;
import org.okapi.wal.LsnSupplier;
import org.okapi.wal.factory.WalResourceBundle;
import org.okapi.wal.factory.WalResourcesFactory;
import org.okapi.wal.io.WalReader;
import org.okapi.wal.io.WalWriter;
import org.okapi.wal.manager.WalManager;

public class ChWalResources {
  Path walDir;
  WalResourceBundle resourceBundle;
  LsnSupplier lsnSupplier;

  public ChWalResources(Path walDir, WalManager.WalConfig config) throws IOException {
    this.walDir = walDir;
    var resourceFactory = new WalResourcesFactory(config);
    this.resourceBundle = resourceFactory.createResourcesFromScratch(this.walDir);
    this.lsnSupplier = resourceBundle.getLsnSupplier();
  }

  public WalWriter getWriter() {
    return this.resourceBundle.getWriter();
  }

  public WalReader getReader() {
    return this.resourceBundle.getReader();
  }

  public WalManager getManager() {
    return this.resourceBundle.getManager();
  }

  public LsnSupplier getSupplier() throws IOException {
    return this.resourceBundle.getLsnSupplier();
  }
}
