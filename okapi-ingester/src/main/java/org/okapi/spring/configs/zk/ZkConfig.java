/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.spring.configs.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.okapi.spring.configs.Profiles;
import org.okapi.spring.configs.properties.OkapiClusterCfg;
import org.okapi.spring.configs.properties.ZkCfg;
import org.okapi.spring.hooks.NodeRegistrationHook;
import org.okapi.zk.ZkClient;
import org.okapi.zk.ZkPaths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@Configuration
public class ZkConfig {

  @Bean
  public CuratorFramework curatorFramework(ZkCfg zkCfg) {
    var client =
        CuratorFrameworkFactory.builder()
            .connectString(zkCfg.getConnectString())
            .retryPolicy(new ExponentialBackoffRetry(zkCfg.getBackoffMillis(), zkCfg.getRetries()))
            .build();
    client.start();
    return client;
  }

  @Bean
  public ZkClient zkClient(@Autowired CuratorFramework curatorFramework) {
    return new ZkClient(curatorFramework);
  }

  @Bean
  public NodeRegistrationHook registrationHook(@Autowired NodeRegistrationHook hook) {
    try {
      hook.registerNode();
    } catch (Exception e) {
      throw new RuntimeException("Failed to register node in Zookeeper", e);
    }
    return hook;
  }

  @Bean
  public ZkPaths zkPaths(@Autowired OkapiClusterCfg clusterCfg) {
    return new ZkPaths(clusterCfg.getNamespace());
  }
}
