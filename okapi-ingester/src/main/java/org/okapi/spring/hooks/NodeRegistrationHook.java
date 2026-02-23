/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.spring.hooks;

import org.okapi.nodes.NodeIdSupplier;
import org.okapi.spring.configs.Profiles;
import org.okapi.zk.ZkClient;
import org.okapi.zk.ZkPaths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class NodeRegistrationHook {

  @Autowired NodeIdSupplier nodeIdSupplier;
  @Autowired ZkClient zkClient;
  @Autowired ZkPaths zkPaths;

  public void registerNode() throws Exception {
    String nodeId = nodeIdSupplier.getNodeId();
    var nodePath = zkPaths.getNodePath(nodeId);
    zkClient.createEphemeral(nodePath, nodeId.getBytes());
  }
}
