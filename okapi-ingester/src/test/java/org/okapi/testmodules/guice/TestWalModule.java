/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
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

public class TestWalModule extends AbstractModule {
  private final Path walRoot;
  private final List<Integer> shards;

  public TestWalModule(Path walRoot, List<Integer> shards) {
    this.walRoot = walRoot;
    this.shards = shards;
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
  WalResourcesPerStream<Integer> provideWalResourcesPerShard(WalResourcesFactory factory)
      throws IOException {
    Files.createDirectories(walRoot);
    var resources = new WalResourcesPerStream<Integer>();
    for (var shard : shards) {
      var bundle = factory.createResourcesFromScratch(walRoot.resolve("shard-" + shard));
      resources.addBundle(shard, bundle);
    }
    return resources;
  }

  @Provides
  @Singleton
  WalResourcesPerStream<String> provideWalResourcesPerStream(WalResourcesFactory factory)
      throws IOException {
    Files.createDirectories(walRoot);
    var resources = new WalResourcesPerStream<String>();
    for (var shard : shards) {
      var bundle = factory.createResourcesFromScratch(walRoot.resolve("bufpool-" + shard));
      resources.addBundle(Integer.toString(shard), bundle);
    }
    return resources;
  }
}
