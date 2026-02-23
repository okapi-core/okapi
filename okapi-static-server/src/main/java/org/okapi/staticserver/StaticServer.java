package org.okapi.staticserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StaticServer {

  public static void main(String[] args) {
    SpringApplication.run(StaticServer.class, args);
  }
}
