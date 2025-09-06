package org.okapi.metrics.spring;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import org.okapi.beans.Configurations;
import org.okapi.clock.Clock;
import org.okapi.metrics.*;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssignerFactory;
import org.okapi.metrics.paths.PathSet;
import org.okapi.metrics.query.promql.PathSetDiscoveryClientFactory;
import org.okapi.metrics.query.promql.RocksMetricsClientFactory;
import org.okapi.metrics.query.promql.SeriesDiscoveryFactory;
import org.okapi.metrics.query.promql.TimeSeriesClientFactory;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.rollup.*;
import org.okapi.metrics.service.MetricsHandlerImpl;
import org.okapi.metrics.service.ServiceController;
import org.okapi.metrics.service.ServiceControllerImpl;
import org.okapi.metrics.service.runnables.*;
import org.okapi.metrics.service.web.QueryProcessor;
import org.okapi.metrics.service.web.RocksQueryProcessor;
import org.okapi.metrics.sharding.ShardPkgManager;
import org.okapi.metrics.stats.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("rocks")
public class RocksWriterConfig {

  @Bean
  public RocksReaderSupplier rocksReaderSupplier(
      @Autowired PathRegistry pathRegistry, @Autowired RocksStore rocksStore) {
    var restorer = new ReadonlyRestorer();
    return new RocksReaderSupplier(pathRegistry, restorer, rocksStore);
  }

  @Bean
  public RocksStore rocksStore() throws IOException {
    return new RocksStore();
  }

  @Bean
  public ServiceController consumerController(
      @Value("${node.user_defined_id}") String id,
      @Qualifier(Configurations.BEAN_ROCKS_MESSAGE_BOX)
          SharedMessageBox<WriteBackRequest> messageBox) {
    return new ServiceControllerImpl(id, messageBox);
  }

  @Bean
  public RocksDbStatsWriter rocksDbStatsWriter(
      @Qualifier(Configurations.BEAN_ROCKS_MESSAGE_BOX)
          SharedMessageBox<WriteBackRequest> requestSharedMessageBox,
      @Autowired PathRegistry pathRegistry)
      throws IOException {
    StatisticsRestorer<UpdatableStatistics> statsRestorer = new WritableRestorer();
    Merger<UpdatableStatistics> statisticsMerger = new RolledupMergerStrategy();
    return new RocksDbStatsWriter(
        requestSharedMessageBox, statsRestorer, statisticsMerger, pathRegistry);
  }

  @Bean
  public PathSet pathSet(@Autowired PathRegistry pathRegistry) {
    return new PathSet(pathRegistry, Collections.emptySet());
  }

  @Bean
  public WriteBackSettings writeBackSettings(
      @Value(Configurations.VAL_WRITE_BACK_WIN_MILLIS) long writeBackMillis,
      @Autowired Clock clock) {
    return new WriteBackSettings(Duration.of(writeBackMillis, ChronoUnit.MILLIS), clock);
  }

  @Bean(name = Configurations.BEAN_ROCKS_MESSAGE_BOX)
  public SharedMessageBox<WriteBackRequest> messageBox(@Value("${rocksBacklog}") int rocksBacklog) {
    return new SharedMessageBox<>(rocksBacklog);
  }

  @Bean(name = Configurations.BEAN_SHARED_EXECUTOR)
  public ScheduledExecutorService scheduledExecutorService(
      @Value("${backgroundThreads}") int backgroundThreads) {
    return Executors.newScheduledThreadPool(backgroundThreads);
  }

  @Bean
  public ShardMap shardMap(
      @Autowired Clock clock,
      @Qualifier(Configurations.BEAN_SERIES_SUPPLIER)
          Function<Integer, RollupSeries<UpdatableStatistics>> seriesFunction,
      @Qualifier(Configurations.BEAN_ROCKS_MESSAGE_BOX)
          SharedMessageBox<WriteBackRequest> requestSharedMessageBox,
      @Qualifier(Configurations.BEAN_SHARED_EXECUTOR) ScheduledExecutorService service,
      @Value(Configurations.VAL_WRITE_BACK_WIN_MILLIS) long writeBackMillis,
      @Value(Configurations.VAL_ADMISSION_WINDOW_HRS) int admissionHrs,
      @Autowired PathSet pathSet)
      throws Exception {
    return new ShardMap(
        clock,
        admissionHrs,
        seriesFunction,
        requestSharedMessageBox,
        service,
        new WriteBackSettings(Duration.of(writeBackMillis, ChronoUnit.MILLIS), clock),
        pathSet);
  }

  @Bean
  public QueryProcessor rocksQueryProcessor(
      @Autowired ShardMap shardMap,
      @Autowired ShardsAndSeriesAssignerFactory shardsAndSeriesAssignerFactory,
      @Autowired RollupQueryProcessor rollupQueryProcessor,
      @Autowired ServiceRegistry serviceRegistry,
      @Autowired RocksReaderSupplier rocksReaderSupplier,
      @Autowired PathSet pathSet,
      @Autowired RocksStore rocksStore,
      @Autowired PathRegistry pathRegistry) {
    return new RocksQueryProcessor(
        shardMap,
        shardsAndSeriesAssignerFactory,
        rollupQueryProcessor,
        serviceRegistry,
        rocksReaderSupplier,
        pathSet,
        rocksStore,
        pathRegistry);
  }

  @Bean
  public TimeSeriesClientFactory timeSeriesClientFactory(
      @Autowired PathRegistry pathRegistry, @Autowired RocksStore rocksStore) {
    var readableRestorer = new ReadonlyRestorer();
    return new RocksMetricsClientFactory(pathRegistry, rocksStore, readableRestorer);
  }

  @Bean
  public SeriesDiscoveryFactory seriesDiscoveryFactory(@Autowired PathSet pathSet) {
    return new PathSetDiscoveryClientFactory(pathSet);
  }

  @Bean
  public MetricsWriter rocksMetricsWriter(
      @Autowired ShardMap shardMap,
      @Autowired ServiceController serviceController,
      @Autowired Node node,
      @Autowired PathSet pathSet) {
    return new RocksMetricsWriter(shardMap, serviceController, node.id(), pathSet);
  }

  @Bean
  public ShardPkgManager shardPkgManager(@Autowired PathRegistry pathRegistry) {
    return new ShardPkgManager(pathRegistry);
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
      @Autowired ShardMap shardMap,
      @Autowired ShardPkgManager shardPkgManager,
      @Autowired RocksStore rocksStore,
      @Autowired TimeSeriesClientFactory timeSeriesClientFactory) {
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
        shardMap,
        shardPkgManager,
        rocksStore,
        timeSeriesClientFactory);
  }

  @Bean
  public FrozenMetricsUploader frozenMetricsUploader(
          @Autowired CheckpointUploaderDownloader checkpointUploaderDownloader,
          @Autowired PathRegistry pathRegistry,
          @Autowired NodeStateRegistry nodeStateRegistry,
          @Autowired Clock clock,
          @Value("${admissionWindowHrs}") long hrs,
          @Autowired PathSet pathSet,
          @Autowired RocksStore rocksStore,
          @Autowired ParquetRollupWriter<UpdatableStatistics> parquetRollupWriter) {
    return new FrozenMetricsUploader(
            checkpointUploaderDownloader,
            pathRegistry,
            nodeStateRegistry,
            clock,
            hrs,
            pathSet,
            rocksStore,
            parquetRollupWriter);
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
  public PathRegistry pathRegistry(
          @Value("${dir.checkpointDir}") String checkpointDir,
          @Value("${dir.shardCheckpointRoot}") String shardCheckpointRoot,
          @Value("${dir.parquetRoot}") String parquetRoot,
          @Value("${dir.shardAssetsRoot}") String shardAssetsRoot)
          throws IOException {
    return PathRegistryImpl.builder()
            .hourlyCheckpointRoot(Path.of(checkpointDir))
            .shardPackageRoot(Path.of(shardCheckpointRoot))
            .parquetRoot(Path.of(parquetRoot))
            .shardAssetsRoot(Path.of(shardAssetsRoot))
            .build();
  }

}
