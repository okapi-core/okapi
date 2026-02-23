package org.okapi.testmodules.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.wal.factory.WalResourcesFactory;
import org.okapi.wal.manager.WalManager;

public class TestLogsWalModule extends AbstractModule {
  private final Path walRoot;
  private final List<Integer> streamIds;

  public TestLogsWalModule(Path walRoot, List<Integer> streamIds) {
    this.walRoot = walRoot;
    this.streamIds = streamIds;
  }

  @Provides
  @Singleton
  WalManager.WalConfig provideWalConfig() {
    return new WalManager.WalConfig(1_048_576);
  }

  @Provides
  @Singleton
  WalResourcesFactory provideWalResourcesFactory(WalManager.WalConfig walConfig) {
    return new WalResourcesFactory(walConfig);
  }

  @Provides
  @Singleton
  WalResourcesPerStream<Integer> provideWalResourcesPerStream(WalResourcesFactory factory)
      throws IOException {
    Files.createDirectories(walRoot);
    var resources = new WalResourcesPerStream<Integer>();
    for (var streamId : streamIds) {
      var bundle = factory.createResourcesFromScratch(walRoot.resolve("bufpool-" + streamId));
      resources.addBundle(streamId, bundle);
    }
    return resources;
  }
}
