package org.okapi.metrics.spring;

import org.okapi.metrics.InMemoryFleetMetadata;
import org.okapi.metrics.IsolatedServiceRegistry;
import org.okapi.metrics.common.FleetMetadata;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.service.self.IsolatedNodeCreator;
import org.okapi.metrics.service.self.NodeCreator;
import org.okapi.metrics.sharding.LeaderJobs;
import org.okapi.metrics.sharding.OwnLeaderJobs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Profile("single")
public class SingleNodeConfig {

  @Bean
  public NodeCreator nodeCreatorIso(@Value("${node.user_defined_id}") String nodeId) {
    return new IsolatedNodeCreator(nodeId);
  }

  @Bean
  public ServiceRegistry serviceRegistryIso(@Autowired Node node) {
    return new IsolatedServiceRegistry(node);
  }
  
  @Bean
  public LeaderJobs leaderJobsNoOp() {
    return new OwnLeaderJobs();
  }
  @Bean
  public FleetMetadata fleetMetadataIso() {
    return new InMemoryFleetMetadata();
  }
}
