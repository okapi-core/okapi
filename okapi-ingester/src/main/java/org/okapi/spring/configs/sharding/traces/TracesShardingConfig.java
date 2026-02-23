/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.spring.configs.sharding.traces;

import org.apache.curator.framework.CuratorFramework;
import org.okapi.identity.WhoAmI;
import org.okapi.sharding.ShardMoveOrchestrator;
import org.okapi.sharding.ShardOrchestrator;
import org.okapi.sharding.listeners.ShardUpdateListener;
import org.okapi.sharding.uploaders.TracesShardUploader;
import org.okapi.spring.configs.Profiles;
import org.okapi.spring.configs.Qualifiers;
import org.okapi.spring.configs.properties.ShardMoveCfg;
import org.okapi.wal.consumer.WalConsumerControllers;
import org.okapi.zk.NamespacedZkClient;
import org.okapi.zk.NamespacedZkClientImpl;
import org.okapi.zk.ZkClient;
import org.okapi.zk.ZkPaths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class TracesShardingConfig {
  @Bean(name = Qualifiers.TRACES_SHARDING_ORCHESTRATOR)
  public ShardOrchestrator shardOrchestratorLogs(
      @Autowired TracesShardUploader tracesShardUploader,
      @Autowired @Qualifier(Qualifiers.TRACES_WAL_CONSUMER_CONTROLLERS)
          WalConsumerControllers walConsumerControllers,
      @Autowired ShardMoveCfg moveCfg) {
    return new ShardMoveOrchestrator(
        tracesShardUploader,
        walConsumerControllers,
        new ShardMoveOrchestrator.ShardMoveConfig(moveCfg.getWalAckDurMillis()));
  }

  @Bean(name = Qualifiers.TRACES_NS_ZK_CLIENT)
  public NamespacedZkClient tracesNsZkClient(
      @Autowired WhoAmI whoAmI, @Autowired ZkPaths zkPaths, @Autowired ZkClient zkClient) {
    return new NamespacedZkClientImpl(zkPaths, zkClient, whoAmI.getNodeId(), ZkPaths.APP.TRACES);
  }

  @Bean(name = Qualifiers.TRACES_SHARD_UPDATE_LISTENER)
  public ShardUpdateListener tracesShardUpdateListener(
      @Autowired @Qualifier(Qualifiers.TRACES_SHARDING_ORCHESTRATOR)
          ShardOrchestrator tracesShardOrchestrator,
      @Autowired @Qualifier(Qualifiers.TRACES_NS_ZK_CLIENT) NamespacedZkClient zkClient,
      @Autowired CuratorFramework curatorFramework,
      @Autowired ZkPaths zkPaths) {
    return new ShardUpdateListener(
        tracesShardOrchestrator,
        zkClient,
        curatorFramework,
        zkPaths.getShardsPath(ZkPaths.APP.TRACES));
  }

  @Bean
  public TracesStartState tracesStartState(
      @Autowired @Qualifier(Qualifiers.TRACES_SHARD_UPDATE_LISTENER)
          ShardUpdateListener shardUpdateListener)
      throws Exception {
    shardUpdateListener.start();
    return new TracesStartState(true, true);
  }
}
