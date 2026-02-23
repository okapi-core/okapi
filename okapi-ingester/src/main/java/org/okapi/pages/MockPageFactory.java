package org.okapi.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.Getter;
import org.okapi.streams.StreamIdentifier;

public class MockPageFactory implements Function<StreamIdentifier<String>, MockAppendPage> {

  long rangeMs;
  long maxBytes;

  @Getter List<String[]> applyCallStack = new ArrayList<>();

  public MockPageFactory(long rangeMs, long maxBytes) {
    this.rangeMs = rangeMs;
    this.maxBytes = maxBytes;
  }

  @Override
  public MockAppendPage apply(StreamIdentifier<String> streamIdentifier) {
    applyCallStack.add(new String[] {streamIdentifier.getStreamId()});
    return new MockAppendPage(rangeMs, maxBytes);
  }
}
