/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.zk;

import static org.okapi.zk.ZkPaths.APP.*;

import org.junit.jupiter.api.Test;

public class ZkPathsTests {

  @Test
  void testPaths() {
    ZkPaths paths = new ZkPaths("namespace");
    assert paths.getRootPath().equals("/okapi/namespace");
    assert paths.getShardsPath(LOGS).equals("/okapi/namespace/LOGS/shards");
    assert paths.getShardPath(TRACES, 1).equals("/okapi/namespace/TRACES/shards/1");
    assert paths.getNodesPath().equals("/okapi/namespace/nodes");
    assert paths.getNodePath("node1").equals("/okapi/namespace/nodes/node1");
  }

  @Test
  void testSeparatedByNameSpace() {
    var paths1 = new ZkPaths("namespace1");
    var paths2 = new ZkPaths("namespace2");
    assert !paths1.getRootPath().equals(paths2.getRootPath());
  }
}
