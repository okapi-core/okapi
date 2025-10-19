package org.okapi.logs.spring;

import java.util.UUID;
import org.okapi.logs.runtime.NodeIdSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NodeIdConfiguration {
  @Bean
  public NodeIdSupplier nodeIdSupplier() {
    String env = System.getenv("HOSTNAME");
    if (env != null && !env.isBlank()) {
      final String id = env;
      return () -> id;
    }
    final String id = UUID.randomUUID().toString();
    return () -> id;
  }
}

