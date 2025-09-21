package org.okapi.metrics;

import com.datastax.oss.driver.api.core.CqlSession;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.Getter;
import lombok.Setter;
import org.okapi.clock.Clock;
import org.okapi.clock.SystemClock;
import org.okapi.fake.FakeClock;
import org.okapi.metrics.cas.*;
import org.okapi.metrics.cas.dao.MetricsMapper;
import org.okapi.metrics.cas.dao.MetricsMapperBuilder;
import org.okapi.metrics.cas.migration.CreateMetricsTableStep;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.ServiceRegistryImpl;
import org.okapi.metrics.common.pojo.*;
import org.okapi.metrics.paths.PathSet;
import org.okapi.metrics.query.promql.*;
import org.okapi.metrics.rollup.*;
import org.okapi.metrics.service.*;
import org.okapi.metrics.service.fakes.*;
import org.okapi.metrics.service.runnables.*;
import org.okapi.metrics.sharding.HeartBeatChecker;
import org.okapi.metrics.sharding.LeaderJobs;
import org.okapi.metrics.sharding.LeaderJobsImpl;
import org.okapi.metrics.singletons.AbstractSingletonFactory;
import org.okapi.metrics.stats.*;
import org.okapi.promql.eval.ts.StatisticsMerger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

public class TestResourceFactory extends AbstractSingletonFactory {

  int nodeCounter = 0;
  Duration checkpointDuration = Duration.of(1, ChronoUnit.HOURS);
  @Getter String dataBucket = "okapi-test-data-bucket";
  public static final String KEYSPACE = "okapi_telemetry";
  Path snapshotDir;
  @Setter @Getter int admissionWindowHrs = 1;
  Path rocksRoot;
  Path metricsPathSetWalRoot;
  @Setter boolean useRealClock;

  public TestResourceFactory() {
    super();
    try {
      snapshotDir = Files.createTempDirectory("snapshot-directory");
      rocksRoot = Files.createTempDirectory("rocks-root");
      metricsPathSetWalRoot = Files.createTempDirectory("metrics-paths-wal");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
            var shardPackageRoot = Files.createTempDirectory(node.id() + "-checkPoints");
            var hourlyCheckpointRoot =
                Files.createTempDirectory(node.id() + "-hourlyCheckpointRoot");
            var parquetRoot = Files.createTempDirectory(node.id() + "-parquet");
            var shardAssetsRoot = Files.createTempDirectory(node.id() + "-shardAssets");
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

  public ScheduledExecutorService scheduledExecutorService(Node node) {
    return makeSingleton(
        node, ScheduledExecutorService.class, () -> Executors.newScheduledThreadPool(10));
  }

  public SharedMessageBox<WriteBackRequest> messageBox(Node node) {
    return makeSingleton(
        node,
        SharedMessageBox.class,
        () -> {
          return new SharedMessageBox<WriteBackRequest>(1000);
        });
  }

  public PathSet pathSet(Node node) throws IOException {
    return makeSingleton(
        node, PathSet.class, () -> new PathSet(pathRegistry(node), Collections.emptySet()));
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

  public Node makeNode(String id) {
    nodeCounter++;
    var ip = "127.0.0." + nodeCounter;
    return new Node(id, ip, NodeState.NODE_CREATED);
  }

  public PathSetDiscoveryClientFactory pathSetSeriesDiscovery(Node node) throws IOException {
    return new PathSetDiscoveryClientFactory(pathSet(node));
  }

  public StatisticsMerger statisticsMerger() {
    return new RollupStatsMerger(new RolledupMergerStrategy());
  }

  public CasMetricsWriter casMetricsWriter(MetricsMapper mapper, Node node) {
    return makeSingleton(
        node,
        CasMetricsWriter.class,
        () ->
            new CasMetricsWriter(
                new KllStatSupplier(),
                mapper.sketchesDao(KEYSPACE),
                mapper.searchHintDao(KEYSPACE),
                mapper.typeHintsDao(KEYSPACE),
                Executors.newFixedThreadPool(50)));
  }

  public CasTsReader casTsReaders(MetricsMapper mapper, Node node) {
    return makeSingleton(
        node,
        CasTsReader.class,
        () ->
            new CasTsReader(
                mapper.sketchesDao(KEYSPACE),
                new KllStatSupplier(),
                new WritableRestorer(),
                new RolledupMergerStrategy()));
  }

  public CasTsSearcher casTsSearcher(MetricsMapper mapper, Node node) {
    return makeSingleton(
        node, CasTsSearcher.class, () -> new CasTsSearcher(mapper.searchHintDao(KEYSPACE)));
  }

  public SeriesDiscoveryFactory casDiscoveryFactory(MetricsMapper mapper, Node node) {
    return makeSingleton(
        node,
        SeriesDiscoveryFactory.class,
        () -> new CasSeriesDiscoveryFactory(casTsSearcher(mapper, node)));
  }

  public TsClientFactory tsClientFactory(Node node) {
    return makeSingleton(node, TsClientFactory.class, () -> new CasTsClientFactory());
  }

  public MetricsMapper metricsMapper() {
    return makeSingleton(
        MetricsMapper.class,
        () -> {
          var session = CqlSession.builder().build();
          MetricsMapper mapper = new MetricsMapperBuilder(session).build();
          var createMetricsTableStep = new CreateMetricsTableStep(session);
          createMetricsTableStep.doStep();

          // check the migration
          mapper.sketchesDao("okapi_telemetry");
          mapper.searchHintDao("okapi_telemetry");
          mapper.typeHintsDao("okapi_telemetry");
          return mapper;
        });
  }
}
