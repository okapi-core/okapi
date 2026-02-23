/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import java.io.IOException;
import java.util.Optional;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.protos.metrics.OffsetAndLen;

public interface BlockSeekIterator extends PageAndMetadataIterator {
  byte[] readBlock(long offset, int length);

  void readOffsetTable() throws IOException, NotEnoughBytesException;

  Optional<OffsetAndLen> getOffsetAndLen(String blockId);

  boolean hasBlock(String blockId);
}
