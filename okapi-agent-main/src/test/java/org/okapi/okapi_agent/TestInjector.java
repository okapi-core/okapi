/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import org.okapi.okapi_agent.jobhandler.HandlerCfgProvider;
import org.okapi.okapi_agent.jobhandler.JobHandlerModule;

public class TestInjector {
  public static Injector createInjector() {
    return createInjectorWithModules(new HandlerCfgProvider(copyHandlerCfg()));
  }

  public static Path copyHandlerCfg() {
    var resourcePath = "handlerCfg/handlerCfg.yaml";
    try (InputStream in = TestInjector.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new IllegalStateException("Missing test resource: " + resourcePath);
      }
      Path tempFile = Files.createTempFile("handlerCfg", ".yml");
      Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
      return tempFile;
    } catch (IOException e) {
      throw new RuntimeException("Failed to copy handler cfg to temp file", e);
    }
  }

  public static Injector createInjectorWithModules(com.google.inject.Module... modules) {
    var allModules = new ArrayList<com.google.inject.Module>();
    allModules.add(new TestFixturesModule());
    allModules.add(new JobHandlerModule());
    allModules.addAll(Arrays.asList(modules));
    return Guice.createInjector(allModules);
  }
}
