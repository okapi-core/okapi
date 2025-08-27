package org.okapi.metrics.sharding;

import lombok.AllArgsConstructor;
import org.okapi.metrics.PathRegistry;
import org.okapi.metrics.rocks.TarPackager;

import java.io.IOException;
import java.nio.file.Path;

@AllArgsConstructor
public class ShardPkgManager {

  PathRegistry pathRegistry;

  public Path packageShard(int shard) throws IOException {
    var packagePath = pathRegistry.shardPackagePath(shard);
    var shardAssetsPath = pathRegistry.shardAssetsPath(shard);
    TarPackager.packageDir(shardAssetsPath, packagePath);
    return packagePath;
  }

  public Path unpackShard(Path tar, int shard) throws IOException {
    var shardsRoot = pathRegistry.shardAssetsRoot();
    TarPackager.unpackTar(tar.toFile(), shardsRoot.toFile());
    return shardsRoot.resolve(Integer.toString(shard));
  }
}
