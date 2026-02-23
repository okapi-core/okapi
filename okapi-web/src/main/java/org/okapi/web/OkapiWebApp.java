/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OkapiWebApp {

  public static void main(String[] args) {
    SpringApplication.run(OkapiWebApp.class, args);
  }
}
