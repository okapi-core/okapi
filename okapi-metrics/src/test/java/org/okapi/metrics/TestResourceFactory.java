package org.okapi.metrics;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.okapi.constants.Constants.N_SHARDS;

import com.google.gson.Gson;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import org.okapi.fake.FakeClock;
import org.okapi.metrics.common.DefaultShardConfig;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.ServiceRegistryImpl;
import org.okapi.metrics.common.ZkPaths;
import org.okapi.metrics.common.pojo.*;
import org.okapi.metrics.rocks.RocksPathSupplier;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.rollup.*;
import org.okapi.metrics.service.*;
import org.okapi.metrics.service.fakes.*;
import org.okapi.metrics.service.runnables.*;
import org.okapi.metrics.service.web.QueryProcessor;
import org.okapi.metrics.sharding.HeartBeatChecker;
import org.okapi.metrics.sharding.LeaderJobs;
import org.okapi.metrics.sharding.LeaderJobsImpl;
import org.okapi.metrics.sharding.fakes.FixedAssignerFactory;
import org.okapi.metrics.stats.RolledupStatsRestorer;
import org.okapi.metrics.stats.RollupSeriesFn;
import org.okapi.metrics.stats.Statistics;
import org.okapi.metrics.stats.StatisticsRestorer;
import org.okapi.wal.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

public class TestResourceFactory {

  int nodeCounter = 0;
  Map<String, Object> singletons;
  Duration checkpointDuration = Duration.of(1, ChronoUnit.HOURS);
  @Getter String dataBucket = "okapi-test-data-bucket";
  Path snapshotDir;
  @Setter @Getter int admissionWindowHrs = 1;
  Path rocksRoot;

  public TestResourceFactory() {
    this.singletons = new HashMap<>();
    try {
      snapshotDir = Files.createTempDirectory("snapshot-directory");
      rocksRoot = Files.createTempDirectory("rocks-root");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public TestResourceFactory(Duration checkpointDuration) {
    this.checkpointDuration = checkpointDuration;
  }

  public InMemoryFleetMetadata fleetMetadata() {
    return makeSingleton(InMemoryFleetMetadata.class, InMemoryFleetMetadata::new);
  }

  public FakeZkResources zkResources(Node node) {
    return makeSingleton(node, FakeZkResources.class, FakeZkResources::new);
  }

  public FakeClock clock(Node node) {
    return makeSingleton(node, FakeClock.class, () -> new FakeClock(10));
  }

  public ServiceRegistry serviceRegistry(Node node) {
    return makeSingleton(
        node,
        ServiceRegistryImpl.class,
        () -> new ServiceRegistryImpl(clock(node), fleetMetadata(), node, zkResources(node)));
  }

  public HeartBeatChecker heartBeatChecker(Node node) {
    return makeSingleton(node, HeartBeatChecker.class, () -> new HeartBeatChecker(clock(node)));
  }

  public LeaderJobs leaderJobs(Node node) {
    return makeSingleton(
        node,
        LeaderJobs.class,
        () ->
            new LeaderJobsImpl(
                serviceRegistry(node), heartBeatChecker(node), zkResources(node), clock(node)));
  }

  public PathRegistry pathRegistry(Node node) {
    return makeSingleton(
        node,
        PathRegistry.class,
        () -> {
          try {
            var shardRoot = Files.createTempDirectory("shardMap");
            var hourlyRoot = Files.createTempDirectory("hourly");
            var parquetRoot = Files.createTempDirectory("parquet");
            return new PathRegistryImpl(shardRoot, hourlyRoot, parquetRoot);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  public S3Client s3Client() {
    return makeSingleton(
        S3Client.class,
        () -> {
          return S3Client.builder()
              .region(Region.US_EAST_1) // LocalStack ignores region but SDK requires it
              .endpointOverride(URI.create("http://localhost:4566")) // LocalStack endpoint
              .credentialsProvider(
                  StaticCredentialsProvider.create(
                      AwsBasicCredentials.create("test", "test") // Dummy creds for LocalStack
                      ))
              .serviceConfiguration(
                  S3Configuration.builder()
                      .pathStyleAccessEnabled(true) // Needed for LocalStack S3
                      .build())
              .build();
        });
  }

  public S3CheckpointUploaderDownloader checkpointUploader(Node node) {
    return makeSingleton(
        S3CheckpointUploaderDownloader.class,
        () -> new S3CheckpointUploaderDownloader(dataBucket, s3Client(), clock(node)));
  }

  public ServiceControllerImpl consumerController(Node node) {
    return makeSingleton(
        node, ServiceControllerImpl.class, () -> new ServiceControllerImpl(node.id()));
  }

  public ImmediateScheduler scheduledExecutorService(Node node) {
    return makeSingleton(node, ImmediateScheduler.class, () -> new ImmediateScheduler(1));
  }

  public HeartBeatReporterRunnable heartBeatReporterRunnable(Node node) {
    return makeSingleton(
        node,
        HeartBeatReporterRunnable.class,
        () ->
            new HeartBeatReporterRunnable(
                serviceRegistry(node), scheduledExecutorService(node), consumerController(node)));
  }

  public LeaderResponsibilityRunnable leaderResponsibilityRunnable(Node node) {
    return makeSingleton(
        node,
        LeaderResponsibilityRunnable.class,
        () ->
            new LeaderResponsibilityRunnable(
                scheduledExecutorService(node), leaderJobs(node), consumerController(node)));
  }

  public SharedMessageBox<WriteBackRequest> messageBox(Node node) {
    return makeSingleton(
        SharedMessageBox.class,
        () -> {
          return new SharedMessageBox<WriteBackRequest>(1000);
        });
  }

  public ShardMap shardMap(Node node) {
    var seriesSupplier = new RollupSeriesFn();
    return makeSingleton(
        node,
        ShardMap.class,
        () ->
            new ShardMap(
                clock(node),
                admissionWindowHrs,
                seriesSupplier,
                messageBox(node),
                scheduledExecutorService(node),
                new WriteBackSettings(Duration.of(100, ChronoUnit.MILLIS), clock(node))));
  }

  public <T> T makeSingleton(Node node, String specifier, Supplier<T> objSupplier) {
    var key = specifier + "/" + node.id();
    if (singletons.containsKey(key)) {
      return (T) singletons.get(key);
    } else {
      singletons.put(key, objSupplier.get());
      return (T) singletons.get(key);
    }
  }

  public <T> T makeSingleton(Node node, Class<T> clazz, Supplier<T> objSupplier) {
    var key = node.id() + "/" + clazz.getSimpleName();
    if (singletons.containsKey(key)) {
      return (T) singletons.get(key);
    } else {
      singletons.put(key, objSupplier.get());
      return (T) singletons.get(key);
    }
  }

  public <T> T makeSingleton(Class<T> clazz, Supplier<T> objSupplier) {
    var key = "/" + clazz.getSimpleName();
    if (singletons.containsKey(key)) {
      return (T) singletons.get(key);
    } else {
      singletons.put(key, objSupplier.get());
      return (T) singletons.get(key);
    }
  }

  public FixedAssignerFactory shardsAndSeriesAssigner() {
    return makeSingleton(FixedAssignerFactory.class, FixedAssignerFactory::new);
  }

  public NodeStateRegistry nodeStateRegistry(Node node) {
    return makeSingleton(
        node, NodeStateRegistryImpl.class, () -> new NodeStateRegistryImpl(fleetMetadata(), node));
  }

  public FrozenMetricsUploader hourlyCheckpointWriter(Node node) {
    return makeSingleton(
        node,
        FrozenMetricsUploader.class,
        () ->
            new FrozenMetricsUploader(
                shardMap(node),
                checkpointUploader(node),
                pathRegistry(node),
                nodeStateRegistry(node),
                clock(node),
                admissionWindowHrs));
  }

  public CheckpointUploader uploaderRunnable(Node node) {
    return makeSingleton(
        node,
        CheckpointUploader.class,
        () ->
            new CheckpointUploader(
                hourlyCheckpointWriter(node),
                scheduledExecutorService(node),
                consumerController(node),
                checkpointDuration));
  }

  public Path walRoot(Node node) {
    return makeSingleton(
        node,
        Path.class,
        () -> {
          try {
            // Create a unique, node-scoped temporary WAL root and cache it
            return Files.createTempDirectory("wal-" + node.id() + "-");
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  public WalBasedMetricsWriter walBasedMetricsWriter(Node node) {
    return makeSingleton(
        node,
        WalBasedMetricsWriter.class,
        () -> {
          try {
            // Ensure the WAL root exists (walRoot(node) now returns a temp dir)
            Path root = walRoot(node);
            Files.createDirectories(root);

            // Framer with explicit LSNs (ShardMap.apply() supplies the monotonic LSN)
            var framer = new ManualLsnWalFramer(24);

            // Build runnable first (without writer), so we can attach commit listener = runnable
            WalBasedMetricsWriter runnable =
                WalBasedMetricsWriter.builder()
                    .shardMap(shardMap(node))
                    .serviceController(consumerController(node))
                    .self(node.id())
                    // FixedAssignerFactory is assumed to implement ShardsAndSeriesAssigner in tests
                    .shardsAndSeriesAssigner(
                        shardsAndSeriesAssigner()
                            .makeAssigner(N_SHARDS, Collections.singletonList(node.id())))
                    .walRoot(root)
                    .walWriter(null) // set after we construct it with listener=runnable
                    .walFramer(framer)
                    .walStreamer(new WalStreamerImpl())
                    .scheduler(scheduledExecutorService(node))
                    .snapshotDelay(checkpointDuration)
                    .cleanerKeepLastK(1)
                    .cleanerGrace(Duration.ZERO)
                    .cleanerDryRun(false)
                    .build();

            // Create a writer bound to this runnable as commit listener, then inject it
            // Create writer with commit listener=runnable, then attach via setter (no reflection)
            var writer =
                new SpilloverWalWriter(
                    new org.okapi.wal.WalAllocator(root),
                    framer,
                    64 * 1024,
                    runnable, // WalCommitListener
                    SpilloverWalWriter.FsyncPolicy.MANUAL,
                    0,
                    null);

            runnable.setWalWriter(writer);
            return runnable;
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  public MetricsHandlerImpl metricsHandler(Node node) {
    return makeSingleton(
        node,
        MetricsHandlerImpl.class,
        () ->
            new MetricsHandlerImpl(
                serviceRegistry(node),
                pathRegistry(node),
                checkpointUploader(node),
                consumerController(node),
                periodicSnapshotWriter(node),
                heartBeatReporterRunnable(node),
                leaderResponsibilityRunnable(node),
                uploaderRunnable(node),
                scheduledExecutorService(node),
                shardsAndSeriesAssigner(),
                shardMap(node)));
  }

  public Node makeNode(String id) {
    nodeCounter++;
    var ip = "127.0.0." + nodeCounter;
    return new Node(id, ip, NodeState.NODE_CREATED);
  }

  public void scaleUp(List<String> nodes, String initial, String opId) {
    assertTrue(nodes.contains(initial));
    var gson = new Gson();
    var scaleupOp =
        new ClusterChangeOp(
            opId,
            nodes,
            TWO_PHASE_STATE.DONE,
            System.currentTimeMillis(),
            System.currentTimeMillis());
    fleetMetadata().setData(ZkPaths.clusterChangeOpPath(), gson.toJson(scaleupOp).getBytes());
    var oldConfig = new ClusterConfig(DefaultShardConfig.OP_ID, Arrays.asList(initial));
    var newNodeConfig = new ClusterConfig(opId, nodes);
    fleetMetadata().setData(ZkPaths.newNodeConfig(), gson.toJson(newNodeConfig).getBytes());
    fleetMetadata().setData(ZkPaths.oldNodeConfig(), gson.toJson(oldConfig).getBytes());
  }

  public RollupQueryProcessor rollupQueryProcessor(Node node) {
    return makeSingleton(node, RollupQueryProcessor.class, RollupQueryProcessor::new);
  }

  public RocksPathSupplier rocksPathSupplier(Node node) {
    return makeSingleton(
        RocksPathSupplier.class,
        () -> {
          var root = rocksRoot.resolve(node.id());
          return new RocksPathSupplier(root);
        });
  }

  public RocksStore rocksStore(Node node) {
    return makeSingleton(
        RocksStore.class,
        () -> {
          try {
            return new RocksStore();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  public StatisticsRestorer<Statistics> unMarshaller() {
    return new RolledupStatsRestorer();
  }

  public RocksReaderSupplier rocksReaderSupplier(Node node) {
    return new RocksReaderSupplier(rocksPathSupplier(node), unMarshaller(), rocksStore(node));
  }

  public QueryProcessor queryProcessor(Node node) {
    return makeSingleton(
        node,
        QueryProcessor.class,
        () ->
            new QueryProcessor(
                shardMap(node),
                shardsAndSeriesAssigner(),
                rollupQueryProcessor(node),
                serviceRegistry(node),
                rocksReaderSupplier(node)));
  }

  public Path snapshotDir(Node node) {
    return makeSingleton(
        node,
        "SnapshotDir",
        () -> {
          var path = snapshotDir.resolve(node.id());
          try {
            Files.createDirectories(path);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return path;
        });
  }

  public Checkpointer checkpointer(Node node) {
    return makeSingleton(
        node,
        Checkpointer.class,
        () -> {
          try {
            return new Checkpointer(
                shardMap(node),
                scheduledExecutorService(node),
                Duration.of(1, ChronoUnit.MINUTES),
                snapshotDir(node),
                clock(node));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  public PeriodicSnapshotWriter periodicSnapshotWriter(Node node) {
    return makeSingleton(
        node,
        PeriodicSnapshotWriter.class,
        () ->
            new PeriodicSnapshotWriter(
                shardMap(node),
                consumerController(node),
                node.id(),
                scheduledExecutorService(node),
                checkpointer(node)));
  }
}
