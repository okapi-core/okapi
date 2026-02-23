package org.okapi.pages;

import org.okapi.byterange.RangeIterationException;

public interface PageAndMetadataIterator {
  boolean hasNextPage();

  byte[] readMetadata() throws RangeIterationException;

  byte[] readPageBody();

  void forward();
}
