/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.spring.configs.sharding.logs;

import org.apache.curator.framework.CuratorFramework;
import org.okapi.identity.WhoAmI;
import org.okapi.sharding.ShardMoveOrchestrator;
import org.okapi.sharding.ShardOrchestrator;
import org.okapi.sharding.listeners.ShardUpdateListener;
import org.okapi.sharding.uploaders.LogsShardUploader;
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

@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@Configuration
public class LogsShardingConfig {
  @Bean(name = Qualifiers.LOGS_SHARDING_ORCHESTRATOR)
  public ShardOrchestrator shardOrchestratorLogs(
      @Autowired LogsShardUploader shardUploader,
      @Autowired @Qualifier(Qualifiers.LOGS_WAL_CONSUMER_CONTROLLERS)
          WalConsumerControllers walConsumerControllers,
      @Autowired ShardMoveCfg moveCfg) {
    return new ShardMoveOrchestrator(
        shardUploader,
        walConsumerControllers,
        new ShardMoveOrchestrator.ShardMoveConfig(moveCfg.getWalAckDurMillis()));
  }

  @Bean(name = Qualifiers.LOGS_NS_ZK_CLIENT)
  public NamespacedZkClient logsZkNsClient(
      @Autowired WhoAmI whoAmI, @Autowired ZkPaths zkPaths, @Autowired ZkClient zkClient) {
    return new NamespacedZkClientImpl(zkPaths, zkClient, whoAmI.getNodeId(), ZkPaths.APP.LOGS);
  }

  @Bean(name = Qualifiers.LOGS_SHARD_UPDATE_LISTENER)
  public ShardUpdateListener logsShardUpdateListener(
      @Autowired @Qualifier(Qualifiers.LOGS_SHARDING_ORCHESTRATOR)
          ShardOrchestrator logsShardOrchestrator,
      @Autowired @Qualifier(Qualifiers.LOGS_NS_ZK_CLIENT) NamespacedZkClient zkClient,
      @Autowired CuratorFramework curatorFramework,
      @Autowired ZkPaths zkPaths) {
    return new ShardUpdateListener(
        logsShardOrchestrator, zkClient, curatorFramework, zkPaths.getShardsPath(ZkPaths.APP.LOGS));
  }

  @Bean
  public LogsStartState logsStartState(
      @Autowired @Qualifier(Qualifiers.LOGS_SHARD_UPDATE_LISTENER)
          ShardUpdateListener shardUpdateListener)
      throws Exception {
    shardUpdateListener.start();
    return new LogsStartState(true, true);
  }
}
