package org.okapi.metrics.cas;

import com.datastax.oss.driver.api.core.CqlSession;
import lombok.Getter;
import org.okapi.metrics.cas.dao.MetricsMapper;
import org.okapi.metrics.cas.dao.MetricsMapperBuilder;
import org.okapi.metrics.cas.migration.CreateMetricsTableStep;

public class CasTesting {
  @Getter MetricsMapper mapper;

  public void bootstrap() {
    var session = CqlSession.builder().build();
    mapper = new MetricsMapperBuilder(session).build();
    var createMetricsTableStep = new CreateMetricsTableStep(session);
    createMetricsTableStep.doStep();

    // check the migration
    mapper.sketchesDao("okapi_telemetry");
    mapper.searchHintDao("okapi_telemetry");
    mapper.typeHintsDao("okapi_telemetry");
  }
}
