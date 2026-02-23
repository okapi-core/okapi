package org.okapi.logs;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = {
      "org.okapi.runtime",
      "org.okapi.logs",
      "org.okapi.traces",
      "org.okapi.metrics",
      "org.okapi.spring",
      "org.okapi.kafka",
      "org.okapi.sharding",
      "org.okapi.promql"
    })
@EnableScheduling
public class TestApplication {}
