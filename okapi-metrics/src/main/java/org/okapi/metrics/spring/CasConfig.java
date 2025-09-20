package org.okapi.metrics.spring;

import com.datastax.oss.driver.api.core.CqlSession;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.okapi.beans.Configurations;
import org.okapi.metrics.cas.*;
import org.okapi.metrics.cas.dao.MetricsMapper;
import org.okapi.metrics.cas.dao.MetricsMapperBuilder;
import org.okapi.metrics.cas.dao.SearchHintDao;
import org.okapi.metrics.cas.dao.SketchesDao;
import org.okapi.metrics.query.promql.SeriesDiscoveryFactory;
import org.okapi.metrics.query.promql.TsClientFactory;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.metrics.rollup.TsSearcher;
import org.okapi.metrics.service.runnables.MetricsWriter;
import org.okapi.metrics.stats.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CasConfig {
  @Bean
  public MetricsWriter metricsWriter(
      @Autowired SketchesDao sketchesDao,
      @Autowired SearchHintDao searchHintDao,
      @Value(Configurations.VAL_CAS_ASYNC_THREADS) int asyncThreads) {
    var executor = Executors.newFixedThreadPool(asyncThreads);
    return new CasMetricsWriter(new KllStatSupplier(), sketchesDao, searchHintDao, executor);
  }

  @Bean
  public MetricsMapper metricsMapper(
      @Value(Configurations.VAL_CAS_CONTACT_PT) String contactPt,
      @Value(Configurations.VAL_CAS_CONTACT_DATACENTER) String datacenter) {
    var parts = contactPt.split(":");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Expected address of form host:port but got " + contactPt);
    }
    var session =
        CqlSession.builder()
            .withLocalDatacenter(datacenter)
            .addContactPoint(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])))
            .build();
    var mapper = new MetricsMapperBuilder(session).build();
    return mapper;
  }

  @Bean
  public SearchHintDao searchHintDao(
      @Autowired MetricsMapper metricsMapper,
      @Value(Configurations.VAL_METRICS_KEY_SPACE) String keyspace) {
    return metricsMapper.searchHintDao(keyspace);
  }

  @Bean
  public SketchesDao sketchesDao(
      @Autowired MetricsMapper metricsMapper,
      @Value(Configurations.VAL_METRICS_KEY_SPACE) String keyspace) {
    return metricsMapper.sketchesDao(keyspace);
  }

  @Bean
  public TsReader reader(@Autowired SketchesDao sketchesDao) {
    return new CasTsReader(
        sketchesDao, new KllStatSupplier(), new WritableRestorer(), new RolledupMergerStrategy());
  }

  @Bean
  public TsSearcher searcher(@Autowired SearchHintDao searchHintDao) {
    return new CasTsSearcher(searchHintDao);
  }

  @Bean
  public TsClientFactory tsClientFactory() {
    return new CasTsClientFactory();
  }

  @Bean
  public SeriesDiscoveryFactory discoveryFactory() {
    return new CasSeriesDiscoveryFactory();
  }
}
