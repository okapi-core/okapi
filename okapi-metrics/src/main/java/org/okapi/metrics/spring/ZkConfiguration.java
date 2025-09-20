package org.okapi.metrics.spring;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.okapi.clock.Clock;
import org.okapi.ip.IpSupplier;
import org.okapi.metrics.common.*;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.service.self.NodeCreator;
import org.okapi.metrics.service.self.ZkNodeCreator;
import org.okapi.metrics.sharding.HeartBeatChecker;
import org.okapi.metrics.sharding.LeaderJobs;
import org.okapi.metrics.sharding.LeaderJobsImpl;
import org.okapi.profiles.ENV_TYPE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("zk")
public class ZkConfiguration {

  @Bean
  public FleetMetadata fleetMetadata(
      @Autowired CuratorFramework curatorFramework, @Autowired RetryPolicy retryPolicy) {
    return new FleetMetadataImpl(curatorFramework, retryPolicy);
  }

  @Bean
  public RetryPolicy retryPolicy(
      @Value("${zk.backoffBase}") int backOffBase, @Value("${zk.backoffNRetries}") int nTrials) {
    return new ExponentialBackoffRetry(backOffBase, nTrials);
  }

  @Bean
  public CuratorFramework curatorFramework(
      @Value("${zk.connectionString}") String zkConnection,
      @Value("${zk.backoffBase}") int base,
      @Value("${zk.backoffNRetries}") int trial) {
    var client =
        CuratorFrameworkFactory.newClient(zkConnection, new ExponentialBackoffRetry(base, trial));
    client.start();
    return client;
  }

  @Bean
  public InterProcessLock clusterLock(@Autowired CuratorFramework curatorFramework) {
    return new InterProcessMutex(curatorFramework, ZkPaths.clusterLock());
  }

  @Bean
  public LeaderLatch leaderLatch(@Autowired CuratorFramework curatorFramework) {
    return new LeaderLatch(curatorFramework, ZkPaths.metricsProcessorLeader());
  }

  @Bean
  public ZkResources zkResources(
      @Autowired CuratorFramework curatorFramework,
      @Autowired InterProcessLock clusterLock,
      @Autowired LeaderLatch leaderLatch)
      throws Exception {
    var resource = new ZkResourcesImpl(curatorFramework, clusterLock, leaderLatch);
    resource.init();
    return resource;
  }

  @Bean
  public NodeCreator nodeCreatorZk(
      @Autowired ENV_TYPE envType,
      @Value("${zk.node.type}") String basePath,
      @Autowired CuratorFramework curatorFramework,
      @Autowired IpSupplier ipSupplier) {
    return switch (envType) {
      case PROD, TEST -> new ZkNodeCreator(curatorFramework, basePath, ipSupplier);
      default -> throw new IllegalArgumentException("profile not accepted " + envType);
    };
  }

  @Bean
  public ServiceRegistry serviceRegistryZk(
      @Autowired Clock clock,
      @Autowired FleetMetadata fleetMetadata,
      @Autowired Node self,
      @Autowired ZkResources zkResources) {
    return new ServiceRegistryImpl(clock, fleetMetadata, self, zkResources);
  }

  @Bean
  public LeaderJobs leaderJobsZK(
      @Autowired ServiceRegistry serviceRegistry,
      @Autowired HeartBeatChecker beatChecker,
      @Autowired ZkResources zk,
      @Autowired Clock clock) {
    return new LeaderJobsImpl(serviceRegistry, beatChecker, zk, clock);
  }
}
