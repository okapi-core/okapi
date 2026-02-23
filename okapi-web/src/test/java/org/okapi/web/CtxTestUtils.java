/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.okapi.web.ai.utils.TagContentsExtractor;

public class CtxTestUtils {

  public static void assertContainsTagWithContent(String prompt, String tag) {
    var content = TagContentsExtractor.getTagContents(prompt, tag);
    Assertions.assertFalse(content.isEmpty(), "Tag: " + tag + " has no content.");
  }

  public static void assertTagContainsContent(String prompt, String tag, Set<String> expected) {
    var content = new HashSet<>(TagContentsExtractor.getTagContents(prompt, tag));
    Assertions.assertEquals(expected, content);
  }
}
