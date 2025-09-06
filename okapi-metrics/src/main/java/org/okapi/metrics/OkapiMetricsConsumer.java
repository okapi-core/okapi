package org.okapi.metrics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {"org.okapi.metrics.config", "org.okapi.metrics.spring"})
public class OkapiMetricsConsumer {

  public static void main(String[] args) {
    SpringApplication.run(OkapiMetricsConsumer.class, args);
  }
}
