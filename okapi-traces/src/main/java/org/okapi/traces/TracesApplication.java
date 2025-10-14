package org.okapi.traces;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TracesApplication {
  public static void main(String[] args) {
    SpringApplication.run(TracesApplication.class, args);
  }
}
