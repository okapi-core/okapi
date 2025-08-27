package org.okapi.metrics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
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
import org.okapi.clock.Clock;
import org.okapi.clock.SystemClock;
import org.okapi.fake.FakeClock;
import org.okapi.metrics.common.DefaultShardConfig;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.ServiceRegistryImpl;
import org.okapi.metrics.common.ZkPaths;
import org.okapi.metrics.common.pojo.*;
import org.okapi.metrics.paths.PathSet;
import org.okapi.metrics.paths.PersistedSetWalPathSupplier;
import org.okapi.metrics.paths.PersistedSetWalPathSupplierImpl;
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
import org.okapi.metrics.stats.*;
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
  Path metricsPathSetWalRoot;
  @Setter boolean useRealClock;

  public TestResourceFactory() {
    this.singletons = new HashMap<>();
    try {
      snapshotDir = Files.createTempDirectory("snapshot-directory");
      rocksRoot = Files.createTempDirectory("rocks-root");
      metricsPathSetWalRoot = Files.createTempDirectory("metrics-paths-wal");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public TestResourceFactory(Duration checkpointDuration) {
    this.checkpointDuration = checkpointDuration;
  }

  public WriteBackSettings writeBackSettings(Node node) {
    return makeSingleton(
        node,
        WriteBackSettings.class,
        () -> new WriteBackSettings(Duration.of(100, ChronoUnit.MILLIS), clock(node)));
  }

  public InMemoryFleetMetadata fleetMetadata() {
    return makeSingleton(InMemoryFleetMetadata.class, InMemoryFleetMetadata::new);
  }

  public FakeZkResources zkResources(Node node) {
    return makeSingleton(node, FakeZkResources.class, FakeZkResources::new);
  }

  public Clock clock(Node node) {
    return makeSingleton(
        node,
        Clock.class,
        () -> {
          if (useRealClock) {
            return new SystemClock();
          }
          return new FakeClock(10);
        });
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
            var shardPackageRoot = Files.createTempDirectory("checkPoints");
            var hourlyCheckpointRoot = Files.createTempDirectory("hourlyCheckpointRoot");
            var parquetRoot = Files.createTempDirectory("parquet");
            var shardAssetsRoot = Files.createTempDirectory("shardAssets");
            return PathRegistryImpl.builder()
                .parquetRoot(parquetRoot)
                .shardPackageRoot(shardPackageRoot)
                .hourlyCheckpointRoot(hourlyCheckpointRoot)
                .shardAssetsRoot(shardAssetsRoot)
                .build();
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

  public ServiceControllerImpl serviceController(Node node) {
    return makeSingleton(
        node, ServiceControllerImpl.class, () -> new ServiceControllerImpl(node.id()));
  }

  public ImmediateScheduler scheduledExecutorService(Node node) {
    return makeSingleton(node, ImmediateScheduler.class, () -> new ImmediateScheduler(10));
  }

  public HeartBeatReporterRunnable heartBeatReporterRunnable(Node node) {
    return makeSingleton(
        node,
        HeartBeatReporterRunnable.class,
        () ->
            new HeartBeatReporterRunnable(
                serviceRegistry(node), scheduledExecutorService(node), serviceController(node)));
  }

  public LeaderResponsibilityRunnable leaderResponsibilityRunnable(Node node) {
    return makeSingleton(
        node,
        LeaderResponsibilityRunnable.class,
        () ->
            new LeaderResponsibilityRunnable(
                scheduledExecutorService(node), leaderJobs(node), serviceController(node)));
  }

  public SharedMessageBox<WriteBackRequest> messageBox(Node node) {
    return makeSingleton(
        SharedMessageBox.class,
        () -> {
          return new SharedMessageBox<WriteBackRequest>(1000);
        });
  }

  public PersistedSetWalPathSupplier persistedSetWalPathSupplier(Node node) throws IOException {
    var walRoot = metricsPathSetWalRoot.resolve(node.id());
    Files.createDirectories(walRoot);
    return new PersistedSetWalPathSupplierImpl(walRoot);
  }

  public PathSet pathSet(Node node) throws IOException {
    return makeSingleton(
        node,
        PathSet.class,
        () -> {
          try {
            return new PathSet(persistedSetWalPathSupplier(node));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  public ShardMap shardMap(Node node) {
    var seriesSupplier = new RollupSeriesFn();
    return makeSingleton(
        node,
        ShardMap.class,
        () -> {
          try {
            return new ShardMap(
                clock(node),
                admissionWindowHrs,
                seriesSupplier,
                messageBox(node),
                scheduledExecutorService(node),
                writeBackSettings(node),
                pathSet(node));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
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

  public ParquetRollupWriter<Statistics> parquetRollupWriter(Node node) {
    return makeSingleton(
        ParquetRollupWriter.class,
        () -> {
          try {
            return new ParquetRollupWriterImpl<Statistics>(
                pathRegistry(node), pathSet(node), rocksStore(node));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  public FrozenMetricsUploader hourlyCheckpointWriter(Node node) {
    return makeSingleton(
        node,
        FrozenMetricsUploader.class,
        () -> {
          try {
            return new FrozenMetricsUploader(
                checkpointUploader(node),
                pathRegistry(node),
                nodeStateRegistry(node),
                clock(node),
                admissionWindowHrs,
                pathSet(node),
                rocksStore(node),
                parquetRollupWriter(node));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  public CheckpointUploader uploaderRunnable(Node node) {
    return makeSingleton(
        node,
        CheckpointUploader.class,
        () ->
            new CheckpointUploader(
                hourlyCheckpointWriter(node),
                scheduledExecutorService(node),
                serviceController(node),
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

  public RocksMetricsWriter rocksWriter(Node node) {
    return new RocksMetricsWriter(shardMap(node), serviceController(node), node.id());
  }

  public MetricsHandlerImpl metricsHandler(Node node) throws IOException {
    return makeSingleton(
        node,
        MetricsHandlerImpl.class,
        () -> {
          return new MetricsHandlerImpl(
              serviceRegistry(node),
              pathRegistry(node),
              checkpointUploader(node),
              serviceController(node),
              rocksWriter(node),
              heartBeatReporterRunnable(node),
              leaderResponsibilityRunnable(node),
              uploaderRunnable(node),
              scheduledExecutorService(node),
              shardsAndSeriesAssigner(),
              shardMap(node));
        });
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
    return new RocksReaderSupplier(pathRegistry(node), unMarshaller(), rocksStore(node));
  }

  public RocksDbStatsWriter rocksDbStatsWriter(Node node) {
    return makeSingleton(
        node,
        RocksDbStatsWriter.class,
        () -> {
          try {
            return new RocksDbStatsWriter(
                messageBox(node),
                new RolledupStatsRestorer(),
                new RolledupMergerStrategy(),
                pathRegistry(node));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  public QueryProcessor queryProcessor(Node node) {
    return makeSingleton(
        node,
        QueryProcessor.class,
        () -> {
          try {
            return new QueryProcessor(
                shardMap(node),
                shardsAndSeriesAssigner(),
                rollupQueryProcessor(node),
                serviceRegistry(node),
                rocksReaderSupplier(node),
                pathSet(node),
                rocksStore(node),
                pathRegistry(node));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  public void startWriter(Node node) {
    var writer = this.rocksDbStatsWriter(node);
    writer.startWriting(scheduledExecutorService(node), rocksStore(node), writeBackSettings(node));
  }
}
