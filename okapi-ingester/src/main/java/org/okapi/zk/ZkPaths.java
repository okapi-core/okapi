/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.zk;

import com.google.common.base.Preconditions;

public class ZkPaths {
  final String namespace;

  public ZkPaths(String namespace) {
    Preconditions.checkNotNull(namespace);
    this.namespace = namespace;
  }

  public enum APP {
    LOGS,
    TRACES,
    METRICS
  }

  public String getRootPath() {
    return "/okapi/" + namespace;
  }

  public String getShardsPath(APP app) {
    return getRootPath() + "/" + app + "/shards";
  }

  public String getShardPath(APP app, int shardId) {
    return getShardsPath(app) + "/" + shardId;
  }

  public String getNodesPath() {
    return getRootPath() + "/nodes";
  }

  public String getNodePath(String nodeId) {
    return getNodesPath() + "/" + nodeId;
  }
}
