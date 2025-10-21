package org.okapi.logs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"org.okapi.logs", "org.okapi.swim"})
@EnableScheduling
public class LogsApplication {
  public static void main(String[] args) {
    SpringApplication.run(LogsApplication.class, args);
  }
}
