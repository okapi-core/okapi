package org.okapi.metrics.spring;

import org.okapi.clock.Clock;
import org.okapi.metrics.NodeStateRegistry;
import org.okapi.metrics.NodeStateRegistryImpl;
import org.okapi.metrics.common.FleetMetadata;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.service.MetricsHandlerImpl;
import org.okapi.metrics.service.ServiceController;
import org.okapi.metrics.service.runnables.BackgroundJobs;
import org.okapi.metrics.service.runnables.ClusterChangeListener;
import org.okapi.metrics.service.runnables.HeartBeatReporterRunnable;
import org.okapi.metrics.service.runnables.LeaderResponsibilityRunnable;
import org.okapi.metrics.sharding.HeartBeatChecker;
import org.okapi.metrics.sharding.LeaderJobs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.ScheduledExecutorService;

@Profile("sharded")
public class ShardedConfig {

    @Bean
    public NodeStateRegistry nodeStateRegistry(
            @Autowired FleetMetadata fleetMetadata, @Autowired Node node) {
        return new NodeStateRegistryImpl(fleetMetadata, node);
    }

    @Bean
    public ClusterChangeListener clusterChangeListener(
            @Autowired ServiceRegistry serviceRegistry,
            @Autowired ScheduledExecutorService scheduledExecutorService,
            @Autowired MetricsHandlerImpl metricsHandler) {
        return new ClusterChangeListener(serviceRegistry, scheduledExecutorService, metricsHandler);
    }

    @Bean
    public BackgroundJobs backgroundJobs(
            @Autowired ClusterChangeListener clusterChangeListener,
            @Autowired ScheduledExecutorService scheduledExecutorService) {
        return new BackgroundJobs(scheduledExecutorService, clusterChangeListener);
    }
    @Bean
    public HeartBeatChecker heartBeatChecker(@Autowired Clock clock) {
        return new HeartBeatChecker(clock);
    }

    @Bean
    public LeaderResponsibilityRunnable leaderResponsibilityRunnable(
            ScheduledExecutorService scheduledExecutorService,
            LeaderJobs leaderJobs,
            ServiceController controller) {
        return new LeaderResponsibilityRunnable(scheduledExecutorService, leaderJobs, controller);
    }

    @Bean
    public HeartBeatReporterRunnable heartBeatReporterRunnable(
            @Autowired ServiceRegistry serviceRegistry,
            @Autowired ScheduledExecutorService scheduledExecutorService,
            @Autowired ServiceController serviceController) {
        return new HeartBeatReporterRunnable(
                serviceRegistry, scheduledExecutorService, serviceController);
    }

}
