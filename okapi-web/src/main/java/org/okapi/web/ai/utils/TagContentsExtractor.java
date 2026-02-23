/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class TagContentsExtractor {

  public static List<String> getTagContents(String content, String tagName) {
    var tagLen = tagName.length();
    var startTagLen = tagLen + 2;
    var matchingContent = new ArrayList<String>();
    var startTagIdx = -1;
    do {
      startTagIdx = content.indexOf("<" + tagName + ">", 1 + startTagIdx);
      if (startTagIdx == -1) continue;
      var endTagIdx = content.indexOf("</" + tagName + ">", 1 + startTagIdx);
      if (endTagIdx == -1) continue;
      var tagContent = content.substring(startTagIdx + startTagLen, endTagIdx);
      if (tagContent.isBlank()) continue;
      matchingContent.add(tagContent.trim());
    } while (startTagIdx >= 0);
    return matchingContent;
  }

  public static List<String> getContentsInPath(String content, List<String> path) {
    var candidates = new ArrayDeque<String>();
    candidates.add(content);
    for (var tag : path) {
      var nextCandidates = new ArrayDeque<String>();
      while (!candidates.isEmpty()) {
        var candidate = candidates.poll();
        var matches = getTagContents(candidate, tag);
        nextCandidates.addAll(matches);
      }
      candidates = nextCandidates;
    }

    return new ArrayList<>(candidates);
  }
}
