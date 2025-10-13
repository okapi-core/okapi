package org.okapi.metrics.spring;

import com.datastax.oss.driver.api.core.CqlSession;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.okapi.beans.Configurations;
import org.okapi.metrics.cas.*;
import org.okapi.metrics.cas.dao.MetricsMapper;
import org.okapi.metrics.cas.dao.MetricsMapperBuilder;
import org.okapi.metrics.cas.dao.SearchHintDao;
import org.okapi.metrics.cas.dao.SketchesDao;
import org.okapi.metrics.cas.migration.CreateMetricsTableStep;
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

@Slf4j
@Configuration
public class CasConfig {
  @Bean
  public MetricsWriter metricsWriter(
      @Autowired SketchesDao sketchesDao,
      @Autowired SearchHintDao searchHintDao,
      @Autowired org.okapi.metrics.cas.dao.TypeHintsDao typeHintsDao,
      @Value(Configurations.VAL_CAS_ASYNC_THREADS) int asyncThreads) {
    var executor = Executors.newFixedThreadPool(asyncThreads);
    return new CasMetricsWriter(
        new KllStatSupplier(), sketchesDao, searchHintDao, typeHintsDao, executor);
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
    log.info("Creating tables if needed");
    var migration = new CreateMetricsTableStep(session);
    migration.doStep();
    log.info("Done creating tables");
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
  public org.okapi.metrics.cas.dao.TypeHintsDao typeHintsDao(
      @Autowired MetricsMapper metricsMapper,
      @Value(Configurations.VAL_METRICS_KEY_SPACE) String keyspace) {
    return metricsMapper.typeHintsDao(keyspace);
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
  public TsClientFactory tsClientFactory(
      @Autowired TsReader tsReader,
      @Autowired org.okapi.metrics.cas.dao.TypeHintsDao typeHintsDao) {
    return new CasTsClientFactory(tsReader, typeHintsDao);
  }

  @Bean
  public SeriesDiscoveryFactory discoveryFactory(@Autowired TsSearcher searcher) {
    return new CasSeriesDiscoveryFactory(searcher);
  }
}
