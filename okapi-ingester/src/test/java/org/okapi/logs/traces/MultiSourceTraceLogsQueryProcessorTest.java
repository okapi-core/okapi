/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.traces;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// todo:
public class MultiSourceTraceLogsQueryProcessorTest {
  Path dir;

  @BeforeEach
  void setup() throws Exception {
    dir = Files.createTempDirectory("okapi-span-multi");
  }

  @AfterEach
  void cleanup() throws Exception {
    if (dir != null)
      Files.walk(dir)
          .sorted((a, b) -> b.getNameCount() - a.getNameCount())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
              });
  }

  @Test
  void merges_and_dedups_across_sources() throws Exception {}
}
