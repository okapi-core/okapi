package org.okapi.metrics.spring;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import java.util.function.Supplier;
import org.okapi.beans.Configurations;
import org.okapi.ip.IpSupplier;
import org.okapi.metrics.Merger;
import org.okapi.metrics.SharedMessageBox;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.fdb.*;
import org.okapi.metrics.query.promql.TimeSeriesClientFactory;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.metrics.service.self.NodeCreator;
import org.okapi.metrics.service.self.UniqueNodeCreator;
import org.okapi.metrics.service.web.QueryProcessor;
import org.okapi.metrics.stats.StatisticsRestorer;
import org.okapi.metrics.stats.UpdatableStatistics;
import org.okapi.rest.metrics.SubmitMetricsRequestInternal;
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
  public SharedMessageBox<SubmitMetricsRequestInternal> executor() {
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
  public QueryProcessor foundationQueryProcessor(@Autowired TsReader reader) {
    return new FdbQueryProcessor(reader);
  }

  @Bean
  public FdbWriter fdbWriter(
      @Qualifier(Configurations.BEAN_FDB_MESSAGE_BOX) @Autowired
          SharedMessageBox<SubmitMetricsRequestInternal> messageBox,
      @Autowired Node node,
      @Autowired Supplier<UpdatableStatistics> supplier,
      @Autowired StatisticsRestorer<UpdatableStatistics> unmarshaller,
      @Autowired Merger<UpdatableStatistics> merger,
      @Autowired Database database) {
    return new FdbWriter(messageBox, merger, database, node, supplier, unmarshaller);
  }

  @Bean
  public TsReader fdbTsReader(
      @Autowired Database database,
      @Autowired StatisticsRestorer<UpdatableStatistics> unmarshaller,
      @Autowired Merger<UpdatableStatistics> merger) {
    return new FdbTsReader(database, merger, unmarshaller);
  }

  @Bean
  public TimeSeriesClientFactory timeSeriesClientFactory(@Autowired TsReader fdbTsReader) {
    return new FdbTsClientFactory(fdbTsReader);
  }

  @Bean
  public FdbSeriesDiscoveryFactory seriesDiscoveryFactory(){
    return new FdbSeriesDiscoveryFactory();
  }
}
