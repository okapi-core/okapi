package org.okapi.zk;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.okapi.exceptions.ExceptionUtils;
import org.okapi.sharding.ShardMetadata;

@Slf4j
public class NamespacedZkClientImpl implements NamespacedZkClient {

  private final ZkPaths zkPaths;
  private final ZkClient client;
  private final Gson gson;
  private final String nodeId; // this process's logical node id
  private final ZkPaths.APP app;

  public NamespacedZkClientImpl(ZkPaths zkPaths, ZkClient client, String nodeId, ZkPaths.APP app) {
    this.zkPaths = zkPaths;
    this.client = client;
    this.gson = new Gson();
    this.nodeId = nodeId;
    this.app = app;
  }

  /** Return all shard ids whose ShardMetadata.owner == this.nodeId. */
  @Override
  public List<Integer> getMyShards() {
    String shardsPath = zkPaths.getShardsPath(app);
    List<String> children;
    try {
      children = client.getChildren(shardsPath);
    } catch (Exception e) {
      // if /shards doesn't exist yet, treat as no assignments
      throw new RuntimeException("Failed to list shard children under " + shardsPath, e);
    }

    if (children.isEmpty()) {
      return List.of();
    }

    List<Integer> result = new ArrayList<>(children.size());

    for (String child : children) {
      int shardId;
      try {
        shardId = Integer.parseInt(child);
      } catch (NumberFormatException nfe) {
        continue;
      }

      String shardPath = zkPaths.getShardPath(app, shardId);
      try {
        byte[] bytes = client.readNode(shardPath);
        ShardMetadata md = gson.fromJson(new String(bytes), ShardMetadata.class);

        if (nodeId.equals(md.getOwner())) {
          result.add(shardId);
        }
      } catch (Exception e) {
        log.error(
            "Failed to read metadata for shard {} at {}: {}",
            shardId,
            shardPath,
            ExceptionUtils.debugFriendlyMsg(e));
        throw new RuntimeException(e);
      }
    }

    return result;
  }

  @Override
  public ShardMetadata getShardState(int shardId) {
    String path = zkPaths.getShardPath(app, shardId);
    try {
      byte[] data = client.readNode(path);
      ShardMetadata md = gson.fromJson(new String(data), ShardMetadata.class);
      return md;
    } catch (KeeperException.NoNodeException e) {
      throw new IllegalStateException("Shard znode does not exist for id " + shardId, e);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get shard state for shard " + shardId, e);
    }
  }
}
