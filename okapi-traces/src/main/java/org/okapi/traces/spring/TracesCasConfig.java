package org.okapi.traces.spring;

import com.datastax.oss.driver.api.core.CqlSession;
import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.okapi.beans.Configurations;
import org.okapi.traces.cas.dao.TracesDao;
import org.okapi.traces.cas.dao.TracesMapper;
import org.okapi.traces.cas.dao.TracesMapperBuilder;
import org.okapi.traces.migration.CreateTraceTablesStep;
import org.okapi.traces.storage.TraceRepository;
import org.okapi.traces.storage.dao.DaoTraceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TracesCasConfig {

  @Bean
  public TracesMapper tracesMapper(
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
    log.info("Creating tables if needed");
    var migration = new CreateTraceTablesStep(session);
    migration.doStep();
    log.info("Finished creating tables");
    return new TracesMapperBuilder(session).build();
  }

  @Bean
  public TracesDao tracesDao(
      TracesMapper mapper, @Value("${cas.traces.keyspace}") String keyspace) {
    return mapper.tracesDao(keyspace);
  }

  @Bean
  public TraceRepository traceRepository(TracesDao dao) {
    return new DaoTraceRepository(dao);
  }
}
