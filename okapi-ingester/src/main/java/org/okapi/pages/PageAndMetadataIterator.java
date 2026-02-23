/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import org.okapi.byterange.RangeIterationException;

public interface PageAndMetadataIterator {
  boolean hasNextPage();

  byte[] readMetadata() throws RangeIterationException;

  byte[] readPageBody();

  void forward();
}
