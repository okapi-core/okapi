package org.okapi.metrics.spring;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import org.okapi.beans.Configurations;
import org.okapi.ip.IpSupplier;
import org.okapi.metrics.Merger;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.fdb.*;
import org.okapi.metrics.query.promql.TsClientFactory;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.metrics.rollup.TsSearcher;
import org.okapi.metrics.service.self.NodeCreator;
import org.okapi.metrics.service.self.UniqueNodeCreator;
import org.okapi.metrics.service.web.QueryProcessor;
import org.okapi.metrics.stats.StatisticsRestorer;
import org.okapi.metrics.stats.UpdatableStatistics;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("fdb")
@Configuration
public class FdbConfig {

  @Bean
  public NodeCreator nodeCreatorIso(IpSupplier ipSupplier) {
    return new UniqueNodeCreator(ipSupplier);
  }

  @Bean(name = Configurations.BEAN_FDB_MESSAGE_BOX)
  public SharedMessageBox<ExportMetricsRequest> executor() {
    return new SharedMessageBox<>(10000);
  }

  @Bean
  public Database database(@Value("fdblib") String fdblib) {
    System.load("/usr/local/lib/libfdb_c.dylib");
    var fdb = FDB.selectAPIVersion(720);
    var db = fdb.open();
    return db;
  }

  @Bean
  public FdbWriter fdbWriter(
      @Qualifier(Configurations.BEAN_FDB_MESSAGE_BOX) @Autowired SharedMessageBox<FdbTx> messageBox,
      @Autowired Database database) {
    return new FdbWriter(database, messageBox);
  }

  @Bean
  public TsReader fdbTsReader(
      @Autowired Database database,
      @Autowired StatisticsRestorer<UpdatableStatistics> unmarshaller,
      @Autowired Merger<UpdatableStatistics> merger) {
    return new FdbTsReader(database, merger, unmarshaller);
  }

  @Bean
  public TsClientFactory timeSeriesClientFactory(@Autowired TsReader fdbTsReader) {
    return new FdbTsClientFactory(fdbTsReader);
  }

  @Bean
  public TsSearcher tsSearcher(@Autowired Database database) {
    return new FdbTsSearcher(database);
  }

  @Bean
  public FdbSeriesDiscoveryFactory seriesDiscoveryFactory(@Autowired TsSearcher tsSearcher) {
    return new FdbSeriesDiscoveryFactory(tsSearcher);
  }

  @Bean
  public QueryProcessor foundationQueryProcessor(
      @Autowired TsReader reader, @Autowired TsSearcher searcher) {
    return new FdbQueryProcessor(reader, searcher);
  }
}
