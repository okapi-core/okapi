package org.okapi.metrics.cas.migration;

import com.datastax.oss.driver.api.core.CqlSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.resourcereader.ClasspathResourceReader;

@Slf4j
@AllArgsConstructor
public class CreateMetricsTableStep {
  public static final String SEP = "--";

  CqlSession session;

  public void doStep() {
    var stmtCreateGaugeSketches = ClasspathResourceReader.readResource("cas/create_tables.cql");
    var split = stmtCreateGaugeSketches.split(SEP);
    for (var sp : split) {
      log.info("Executing..\n" + sp);
      session.execute(sp);
      log.info("Done");
    }
  }
}
