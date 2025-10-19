package org.okapi.logs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LogsApplication {
  public static void main(String[] args) {
    SpringApplication.run(LogsApplication.class, args);
  }
}

