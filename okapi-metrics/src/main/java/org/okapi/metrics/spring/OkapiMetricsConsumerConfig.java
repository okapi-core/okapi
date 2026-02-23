package org.okapi.metrics.spring;

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
import org.okapi.nodes.Ec2IpSupplier;
import org.okapi.nodes.FixedIpSupplier;
import org.okapi.nodes.IpSupplier;
import org.okapi.metrics.*;
import org.okapi.metrics.aws.NoOpCredentials;
import org.okapi.metrics.query.QueryProcImpl;
import org.okapi.metrics.query.promql.PathSetDiscoveryClientFactory;
import org.okapi.metrics.query.promql.PromQlQueryProcessor;
import org.okapi.metrics.query.promql.RollupStatsMerger;
import org.okapi.promql.runtime.SeriesDiscoveryFactory;
import org.okapi.promql.runtime.TsClientFactory;
import org.okapi.metrics.rollup.*;
import org.okapi.metrics.service.web.QueryProcessor;
import org.okapi.metrics.stats.*;
import org.okapi.profiles.ENV_TYPE;
import org.okapi.rest.promql.Sample;
import org.okapi.rest.promql.SampleAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
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
  public IpSupplier ipSupplier(@Autowired ENV_TYPE envType) {
    return switch (envType) {
      case TEST, INTEG_TEST, ISO -> new FixedIpSupplier("localhost");
      case PROD -> new Ec2IpSupplier();
    };
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

  @Bean
  public ProtobufHttpMessageConverter protobufHttpMessageConverter() {
    return new ProtobufHttpMessageConverter();
  }

  @Bean
  public QueryProcessor queryProcessor(
      @Autowired TsReader tsReader, @Autowired TsSearcher tsSearcher) {
    return new QueryProcImpl(tsReader, tsSearcher);
  }
}
