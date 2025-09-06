package org.okapi.metrics.spring.async;

import org.okapi.metrics.fdb.FdbWriter;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FdbConsumerJob {

  @Autowired
  FdbWriter fdbWriter;

  @Scheduled(fixedRate = 1000)
  public void consumeOnce() throws StatisticsFrozenException {
    fdbWriter.writeOnce();
  }
}
