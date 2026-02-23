/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.utils;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TagContentsExtractorTests {

  @Test
  void someContent() {
    var content = "<tag>content</tag>";
    var tagContent = TagContentsExtractor.getTagContents(content, "tag");
    Assertions.assertEquals(List.of("content"), tagContent);
  }

  @Test
  void noContent() {
    var content = "<tag></tag>";
    var extracted = TagContentsExtractor.getTagContents(content, "tag");
    Assertions.assertTrue(extracted.isEmpty());
  }

  @Test
  void noMatchingTag() {
    var content = "<tag1></tag1>";
    var extracted = TagContentsExtractor.getTagContents(content, "tag");
    Assertions.assertTrue(extracted.isEmpty());
  }

  @Test
  void malformed_noStartTag() {
    var content = "<tag></tag1>";
    var extracted = TagContentsExtractor.getTagContents(content, "tag");
    Assertions.assertTrue(extracted.isEmpty());
  }

  @Test
  void malformed_noEndTag() {
    var content = "<tag1></tag>";
    var extracted = TagContentsExtractor.getTagContents(content, "tag");
    Assertions.assertTrue(extracted.isEmpty());
  }

  @Test
  void multipleContentMatch() {
    var content = "<tag>content-1</tag><tag>content-2</tag>";
    var extracted = TagContentsExtractor.getTagContents(content, "tag");
    Assertions.assertTrue(extracted.contains("content-1"));
    Assertions.assertTrue(extracted.contains("content-2"));
  }

  @Test
  void pathExtract() {
    var content = "<a><b><c>value-1</c><c>value-2</c></b></a><a><b><c>value-3</c></b></a>";
    var extracted = TagContentsExtractor.getContentsInPath(content, List.of("a", "b", "c"));
    Assertions.assertEquals(List.of("value-1", "value-2", "value-3"), extracted);
  }

  @Test
  void pathExtract_noMatch() {
    var content = "<a><b><c>value-1</c><c>value-2</c></b></a><a><b><c>value-3</c></b></a>";
    var extracted = TagContentsExtractor.getContentsInPath(content, List.of("a", "x", "c"));
    Assertions.assertTrue(extracted.isEmpty());
  }

  @Test
  void pathExtract_partialMatch() {
    var content = "<a><b><c>value-1</c><c>value-2</c></b></a><a><b><c>value-3</c></b></a>";
    var extracted = TagContentsExtractor.getContentsInPath(content, List.of("a", "b", "x"));
    Assertions.assertTrue(extracted.isEmpty());
  }

  @Test
  void pathExtract_multipleBlocks() {
    var content = "<a><b><c>value-1</c><c>value-2</c></b></a><a><b><c>value-3</c></b></a>";
    var extracted = TagContentsExtractor.getContentsInPath(content, List.of("a", "b"));
    Assertions.assertEquals(List.of("<c>value-1</c><c>value-2</c>", "<c>value-3</c>"), extracted);
  }
}
