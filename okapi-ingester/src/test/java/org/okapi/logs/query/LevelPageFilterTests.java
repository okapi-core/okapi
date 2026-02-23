package org.okapi.logs.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.logs.io.LogPageMetadata;

public class LevelPageFilterTests {
  LogPageMetadata metadata;

  @BeforeEach
  void setup() {
    metadata = LogPageMetadata.createEmptyMetadata(1000);
    metadata.putLogLevel(40);
  }

  @Test
  void testShouldRead() {
    var filter = new LevelPageFilter(40);
    assertTrue(filter.shouldReadPage(metadata));
  }

  @Test
  void testShouldSkip() {
    var filter = new LevelPageFilter(10);
    assertFalse(filter.shouldReadPage(metadata));
  }
}
