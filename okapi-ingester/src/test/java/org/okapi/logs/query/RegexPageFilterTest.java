/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.abstractio.TrigramUtil;
import org.okapi.logs.io.LogPageMetadata;

public class RegexPageFilterTest {

  LogPageMetadata metadata;

  @BeforeEach
  void setup() {
    var body = "This is an ERROR log message.";
    metadata = LogPageMetadata.createEmptyMetadata(1000);
    var trigrams = TrigramUtil.extractAsciiTrigramIndices(body);
    for (var trigram : trigrams) {
      metadata.putLogBodyTrigram(trigram);
    }
  }

  @Test
  void testReadsIfNecessary() {
    var filter = new RegexPageFilter(".*ERROR.*");
    assertTrue(filter.shouldReadPage(metadata));
  }

  @Test
  void testReadsIfNecessary_orQuery() {
    var filter = new RegexPageFilter(".*ERROR.*|.*WARNING.*");
    assertTrue(filter.shouldReadPage(metadata));
  }

  @Test
  void testReadsIfNecessary_orQueryBothMatch() {
    var filter = new RegexPageFilter(".*ERROR.*|.*log.*");
    assertTrue(filter.shouldReadPage(metadata));
  }

  @Test
  void testShouldReadSkips() {
    var filter = new RegexPageFilter(".*ERRORS.*");
    assertFalse(filter.shouldReadPage(metadata));
  }
}
