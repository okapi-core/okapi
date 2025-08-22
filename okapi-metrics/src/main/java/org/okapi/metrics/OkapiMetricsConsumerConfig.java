package org.okapi.metrics;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.okapi.beans.BeanIds;
import org.okapi.clock.Clock;
import org.okapi.clock.SystemClock;
import org.okapi.fake.FakeClock;
import org.okapi.ip.Ec2IpSupplier;
import org.okapi.ip.FixedIpSupplier;
import org.okapi.ip.IpSupplier;
import org.okapi.metrics.aws.NoOpCredentials;
import org.okapi.metrics.common.*;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.sharding.ConsistentHashedAssignerFactory;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssignerFactory;
import org.okapi.metrics.coordinator.CentralCoordinator;
import org.okapi.metrics.rollup.FrozenMetricsUploader;
import org.okapi.metrics.rollup.RollupQueryProcessor;
import org.okapi.metrics.service.*;
import org.okapi.metrics.service.runnables.*;
import org.okapi.metrics.service.self.IsolatedNodeCreator;
import org.okapi.metrics.service.self.NodeCreator;
import org.okapi.metrics.service.self.ZkNodeCreator;
import org.okapi.metrics.sharding.*;
import org.okapi.metrics.stats.*;
import org.okapi.profiles.ENV_TYPE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Contrary to popular advice, we go with one giant config per service. This is to eliminate
 * reasoning about the origin of a bean that shows up in your Spring application. Single source of
 * truth eliminates this need. Other than the controllers, autowired is used nowhere else.
 */
@Slf4j
@Configuration
public class OkapiMetricsConsumerConfig {
  @Autowired Environment environment;

  @Bean
  public ENV_TYPE env() {
    var profiles = environment.getActiveProfiles()[0];
    return ENV_TYPE.parse(profiles);
  }

  @Bean
  @ConditionalOnProperty(
      name = "remoteHourlyUploads", // property to check
      havingValue = "enabled", // required value
      matchIfMissing = false)
  public S3Client amazonS3v2(
      @Autowired ENV_TYPE ENV_TYPE,
      @Value(BeanIds.VALUE_REGION) String region,
      @Autowired AwsCredentialsProvider credentialsProviderV2) {

    return switch (ENV_TYPE) {
      case PROD ->
          S3Client.builder()
              .region(Region.of(region))
              .credentialsProvider(credentialsProviderV2)
              .build();

      case TEST, INTEG_TEST, ISO ->
          S3Client.builder()
              .endpointOverride(URI.create("http://localhost:4566"))
              .region(Region.of(region))
              .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
              .credentialsProvider(credentialsProviderV2)
              .build();
    };
  }

  @Bean
  public PathRegistry pathRegistry(
      @Value("${dir.checkpointDir}") String checkpointDir,
      @Value("${dir.shardCheckpointRoot}") String shardCheckpointRoot,
      @Value("${dir.parquetRoot}") String parquetRoot)
      throws IOException {
    return PathRegistryImpl.builder()
        .checkpointUploaderRoot(Path.of(checkpointDir))
        .shardCheckpointRoot(Path.of(shardCheckpointRoot))
        .parquetRoot(Path.of(parquetRoot))
        .build();
  }

  @Bean
  public ServiceController consumerController(@Value("${node.user_defined_id}") String id) {
    return new ServiceControllerImpl(id);
  }

  @Bean
  public RetryPolicy retryPolicy(
      @Value("${zk.backoffBase}") int backOffBase, @Value("${zk.backoffNRetries}") int nTrials) {
    return new ExponentialBackoffRetry(backOffBase, nTrials);
  }

  @Bean
  @ConditionalOnProperty(
      name = "mode", // property to check
      havingValue = "zk", // required value
      matchIfMissing = false)
  public FleetMetadata fleetMetadata(
      @Autowired CuratorFramework curatorFramework, @Autowired RetryPolicy retryPolicy) {
    return new FleetMetadataImpl(curatorFramework, retryPolicy);
  }

  @Bean
  @ConditionalOnProperty(
      name = "mode", // property to check
      havingValue = "isolated", // required value
      matchIfMissing = true)
  public FleetMetadata fleetMetadataIso() {
    return new InMemoryFleetMetadata();
  }

  @Bean
  @ConditionalOnProperty(
      name = "remoteHourlyUploads", // property to check
      havingValue = "enabled")
  public CheckpointUploaderDownloader checkpointUploaderDownloaderS3(
      @Autowired Clock clock, @Value("${dataBucket}") String dataBucket, @Autowired S3Client s3) {
    return new S3CheckpointUploaderDownloader(dataBucket, s3, clock);
  }

  @Bean
  @ConditionalOnProperty(
      name = "remoteHourlyUploads", // property to check
      havingValue = "disabled")
  public CheckpointUploaderDownloader checkpointUploaderDownloaderNoOp() {
    return new NoOpCheckpointUploader();
  }

  @Bean
  public Clock clock(@Autowired ENV_TYPE envType) {
    return switch (envType) {
      case PROD -> new SystemClock();
      case INTEG_TEST -> new FakeClock(100);
      case TEST, ISO -> new SystemClock();
    };
  }

  @Bean
  // todo: make this pluggable
  public ShardMap shardMap(@Autowired Clock clock, @Value("${admissionWindowHrs}") int admissionHrs)
      throws Exception {
    StatisticsRestorer<Statistics> statsRestorer = new RolledupStatsRestorer();
    var statsSupplier = new KllStatSupplier();
    var seriesRestorer = new RolledUpSeriesRestorer( statsRestorer, statsSupplier);
    return new ShardMap(
        clock,
        admissionHrs,
        statsSupplier,
        statsRestorer,
        seriesRestorer);
    }

  @Bean
  public ShardsAndSeriesAssignerFactory shardsAndSeriesAssignerFactory() {
    return new ConsistentHashedAssignerFactory();
  }

  @Bean
  @ConditionalOnProperty(
      name = "mode", // property to check
      havingValue = "zk", // required value
      matchIfMissing = false)
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
  @ConditionalOnProperty(
      name = "mode", // property to check
      havingValue = "zk")
  public InterProcessLock clusterLock(@Autowired CuratorFramework curatorFramework) {
    return new InterProcessMutex(curatorFramework, ZkPaths.clusterLock());
  }

  @Bean
  @ConditionalOnProperty(
      name = "mode", // property to check
      havingValue = "zk")
  public LeaderLatch leaderLatch(@Autowired CuratorFramework curatorFramework) {
    return new LeaderLatch(curatorFramework, ZkPaths.metricsProcessorLeader());
  }

  @ConditionalOnProperty(
      name = "mode", // property to check
      havingValue = "zk")
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
  public ScheduledExecutorService scheduledExecutorService(
      @Value("${backgroundThreads}") int backgroundThreads) {
    return Executors.newScheduledThreadPool(backgroundThreads);
  }

  @Bean
  public IpSupplier ipSupplier(@Autowired ENV_TYPE envType, @Value("${server.port}") int port) {
    return switch (envType) {
      case TEST, INTEG_TEST, ISO -> new FixedIpSupplier("localhost", port);
      case PROD -> new Ec2IpSupplier(port);
    };
  }

  @Bean
  @ConditionalOnProperty(
      name = "mode", // property to check
      havingValue = "zk")
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
  @ConditionalOnProperty(
      name = "mode", // property to check
      havingValue = "isolated")
  public NodeCreator nodeCreatorIso(@Value("${node.user_defined_id}") String nodeId) {
    return new IsolatedNodeCreator(nodeId);
  }

  @Bean
  public Node self(@Autowired NodeCreator nodeCreator) {
    return nodeCreator.whoAmI();
  }

  @Bean
  public RollupQueryProcessor rollupQueryProcessor() {
    return new RollupQueryProcessor();
  }

  @Bean
  @ConditionalOnProperty(
      name = "mode", // property to check
      havingValue = "zk" // required value
      )
  public ServiceRegistry serviceRegistryZk(
      @Autowired Clock clock,
      @Autowired FleetMetadata fleetMetadata,
      @Autowired Node self,
      @Autowired ZkResources zkResources) {
    return new ServiceRegistryImpl(clock, fleetMetadata, self, zkResources);
  }

  @Bean
  @ConditionalOnProperty(
      name = "mode", // property to check
      havingValue = "isolated", // required value
      matchIfMissing = true // if property missing → bean not created
      )
  public ServiceRegistry serviceRegistryIso(@Autowired Node node) {
    return new IsolatedServiceRegistry(node);
  }

  @Bean
  public AwsCredentialsProvider credentialsProviderV2(@Autowired ENV_TYPE ENV_TYPE) {
    return switch (ENV_TYPE) {
      case PROD ->
          software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider.create();
      case TEST ->
          software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider.create();
      case ISO -> new NoOpCredentials();
      case INTEG_TEST ->
          software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider.create();
    };
  }

  @Bean
  public HeartBeatReporterRunnable heartBeatReporterRunnable(
      @Autowired ServiceRegistry serviceRegistry,
      @Autowired ScheduledExecutorService scheduledExecutorService,
      @Autowired ServiceController serviceController) {
    return new HeartBeatReporterRunnable(
        serviceRegistry, scheduledExecutorService, serviceController);
  }

  @Bean
  @ConditionalOnProperty(name = "mode", havingValue = "zk")
  public LeaderJobs leaderJobsZK(
      @Autowired ServiceRegistry serviceRegistry,
      @Autowired HeartBeatChecker beatChecker,
      @Autowired ZkResources zk,
      @Autowired Clock clock) {
    return new LeaderJobsImpl(serviceRegistry, beatChecker, zk, clock);
  }

  @Bean
  @ConditionalOnProperty(name = "mode", havingValue = "isolated")
  public LeaderJobs leaderJobsNoOp() {
    return new OwnLeaderJobs();
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
  public CheckpointUploader hourlyCheckpointUploaderRunnable(
      @Autowired FrozenMetricsUploader frozenMetricsUploader,
      @Autowired ScheduledExecutorService scheduledExecutorService,
      @Autowired ServiceController serviceController,
      @Value("${remoteCkptDelaySeconds}") int checkpointSeconds) {
    return new CheckpointUploader(
        frozenMetricsUploader,
        scheduledExecutorService,
        serviceController,
        Duration.of(checkpointSeconds, ChronoUnit.SECONDS));
  }

  @Bean
  public FrozenMetricsUploader hourlyCheckpointWriter(
      @Autowired ShardMap shardMap,
      @Autowired CheckpointUploaderDownloader checkpointUploaderDownloader,
      @Autowired PathRegistry pathRegistry,
      @Autowired NodeStateRegistry nodeStateRegistry,
      @Autowired Clock clock,
      @Value("${admissionWindowHrs}") long hrs) {
    return new FrozenMetricsUploader(
        shardMap, checkpointUploaderDownloader, pathRegistry, nodeStateRegistry, clock, hrs);
  }

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
  @ConditionalOnProperty(name = "mode", havingValue = "zk")
  public CentralCoordinator centralCoordinator(
      @Autowired ServiceController serviceController,
      @Autowired CuratorFramework curatorFramework,
      @Value("${zk.node.type}") String nodeType) {
    return new CentralCoordinator(serviceController, curatorFramework, nodeType);
  }

  @Bean
  public Checkpointer checkpointer(
      @Autowired ShardMap shardMap,
      @Autowired ScheduledExecutorService executorService,
      @Value("${checkpoint.gapMillis}") long checkpointGapMillis,
      @Value("${checkpoint.dir}") String checkpointDir,
      @Autowired Clock clock)
      throws IOException {

    return new Checkpointer(
        shardMap,
        executorService,
        Duration.of(checkpointGapMillis, ChronoUnit.MILLIS),
        Path.of(checkpointDir),
        clock);
  }

  @Bean
  public MetricsWriter metricsWriter(
      @Autowired ShardMap shardMap,
      @Autowired ServiceController serviceController,
      @Autowired Node node,
      @Autowired ScheduledExecutorService scheduledExecutorService,
      @Autowired Checkpointer checkpointer) {
    return new PeriodicSnapshotWriter(
        shardMap, serviceController, node.id(), scheduledExecutorService, checkpointer);
  }

  @Bean
  public MetricsHandlerImpl metricsHandler(
      @Autowired ServiceRegistry serviceRegistry,
      @Autowired PathRegistry pathRegistry,
      @Autowired CheckpointUploaderDownloader checkpointUploaderDownloader,
      @Autowired ServiceController serviceController,
      @Autowired MetricsWriter writer,
      @Autowired HeartBeatReporterRunnable heartBeatReporterRunnable,
      @Autowired LeaderResponsibilityRunnable leaderResponsibilityRunnable,
      @Autowired CheckpointUploader hourlyCheckpointUploaderRunnable,
      @Autowired ScheduledExecutorService scheduledExecutorService,
      @Autowired ShardsAndSeriesAssignerFactory shardsAndSeriesAssignerFactory,
      @Autowired ShardMap shardMap) {
    return new MetricsHandlerImpl(
        serviceRegistry,
        pathRegistry,
        checkpointUploaderDownloader,
        serviceController,
        writer,
        heartBeatReporterRunnable,
        leaderResponsibilityRunnable,
        hourlyCheckpointUploaderRunnable,
        scheduledExecutorService,
        shardsAndSeriesAssignerFactory,
        shardMap);
  }
}
