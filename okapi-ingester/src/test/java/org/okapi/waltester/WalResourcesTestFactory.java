package org.okapi.waltester;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.wal.factory.WalResourceBundle;
import org.okapi.wal.factory.WalResourcesFactory;
import org.okapi.wal.manager.WalManager;

public class WalResourcesTestFactory {
  public static WalResourceBundle createBundle(Path dir, WalManager.WalConfig config)
      throws IOException {
    var factory = new WalResourcesFactory(config);
    return factory.createResourcesFromScratch(dir);
  }

  public static WalResourceBundle createBundleDefaultConfig(Path dir) throws IOException {
    var factory = new WalResourcesFactory(new WalManager.WalConfig(100L));
    return factory.createResourcesFromScratch(dir);
  }

  public static <Id> WalResourcesPerStream<Id> createResources(Path dir, Set<Id> streamIds)
      throws IOException {
    var resources = new WalResourcesPerStream<Id>();
    for (var sid : streamIds) {
      var root = dir.resolve(sid.toString());
      var bundle = createBundleDefaultConfig(root);
      resources.addBundle(sid, bundle);
    }
    return resources;
  }
  public static WalResourcesPerStream<String> singleStreamSetup(Path dir) throws IOException {
    return WalResourcesTestFactory.createResources(dir, Set.of("0"));
  }
}
