package org.okapi.traces;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TracesApplication {
    // todo: write a Cas or S3 backed design for this project.
    // todo: wire up a spring boot config and controller -> add an integration test based on Otel ingestion and normal queries
    // todo: add some unit-tests for critical logic -> move on to logs
    public static void main(String[] args) {
        SpringApplication.run(TracesApplication.class, args);
    }
}
