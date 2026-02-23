/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import org.okapi.streams.StreamIdentifier;

public class MockPageFlusher implements PageFlusher<MockAppendPage, String> {

  @Getter Map<String, MockAppendPage> flushedPages = new ConcurrentHashMap<>();
  @Setter boolean throwOnFlush = false;

  public MockPageFlusher() {}

  @Override
  public void flush(StreamIdentifier<String> identifier, MockAppendPage page) throws Exception {
    if (throwOnFlush) {
      throw new Exception("MockPageFlusher configured to throw on flush");
    }
    flushedPages.put(identifier.getStreamId(), page);
  }
}
