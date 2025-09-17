package org.okapi.metrics.spring;

import com.apple.foundationdb.Database;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.okapi.beans.Configurations;
import org.okapi.clock.Clock;
import org.okapi.clock.SystemClock;
import org.okapi.fake.FakeClock;
import org.okapi.ip.Ec2IpSupplier;
import org.okapi.ip.FixedIpSupplier;
import org.okapi.ip.IpSupplier;
import org.okapi.metrics.*;
import org.okapi.metrics.aws.NoOpCredentials;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.sharding.ConsistentHashedAssignerFactory;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssignerFactory;
import org.okapi.metrics.fdb.FdbMetricsWriter;
import org.okapi.metrics.fdb.FdbTx;
import org.okapi.metrics.query.promql.*;
import org.okapi.metrics.rollup.*;
import org.okapi.metrics.service.runnables.*;
import org.okapi.metrics.service.self.NodeCreator;
import org.okapi.metrics.stats.*;
import org.okapi.profiles.ENV_TYPE;
import org.okapi.rest.promql.Sample;
import org.okapi.rest.promql.SampleAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

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
  public Clock clock(@Autowired ENV_TYPE envType) {
    return switch (envType) {
      case PROD -> new SystemClock();
      case INTEG_TEST -> new FakeClock(100);
      case TEST, ISO -> new SystemClock();
    };
  }

  @Bean(name = Configurations.BEAN_SERIES_SUPPLIER)
  public Function<Integer, RollupSeries<UpdatableStatistics>> seriesSupplier() {
    StatisticsRestorer<UpdatableStatistics> statsRestorer = new WritableRestorer();
    var statsSupplier = new KllStatSupplier();
    return (shard) -> new RollupSeries<>(statsSupplier, shard);
  }

  @Bean
  public ShardsAndSeriesAssignerFactory shardsAndSeriesAssignerFactory() {
    return new ConsistentHashedAssignerFactory();
  }

  @Bean
  public IpSupplier ipSupplier(@Autowired ENV_TYPE envType, @Value("${server.port}") int port) {
    return switch (envType) {
      case TEST, INTEG_TEST, ISO -> new FixedIpSupplier("localhost", port);
      case PROD -> new Ec2IpSupplier(port);
    };
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
  public Merger<UpdatableStatistics> merger() {
    return new RolledupMergerStrategy();
  }

  @Bean
  public Supplier<UpdatableStatistics> newStats() {
    return new KllStatSupplier();
  }

  @Bean
  public StatisticsRestorer<UpdatableStatistics> restorer() {
    return new WritableRestorer();
  }

  @Bean
  public MetricsWriter metricsWriter(
      @Autowired Database database,
      @Autowired Node node,
      @Qualifier(Configurations.BEAN_FDB_MESSAGE_BOX) @Autowired
          SharedMessageBox<FdbTx> messageBox) {
    return new FdbMetricsWriter(node.id(), messageBox, new KllStatSupplier());
  }

  @Bean(name = Configurations.BEAN_PROMQL_SERIALIZER)
  public Gson promqlSerializer() {
    return new GsonBuilder().registerTypeAdapter(Sample.class, new SampleAdapter()).create();
  }

  @Bean
  public PromQlQueryProcessor promQlQueryProcessor(
      @Autowired TsClientFactory tsClientFactory,
      @Autowired SeriesDiscoveryFactory seriesDiscoveryFactory,
      @Value(Configurations.VAL_PROMQL_EVAL_THREADS) int threads) {
    var singleThreadedExecutor = Executors.newFixedThreadPool(threads);
    var merger = new RollupStatsMerger(new RolledupMergerStrategy());
    return new PromQlQueryProcessor(
        singleThreadedExecutor, merger, tsClientFactory, seriesDiscoveryFactory);
  }
}
