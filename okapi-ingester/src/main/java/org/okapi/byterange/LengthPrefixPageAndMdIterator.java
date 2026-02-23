/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.byterange;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;
import org.okapi.pages.PageAndMetadataIterator;
import org.okapi.s3.ByteRangeSupplier;

public class LengthPrefixPageAndMdIterator implements PageAndMetadataIterator {

  ByteRangeSupplier rangeSupplier;
  long offset = 0;
  int mdLen = 0;
  int docBlockLen = 0;

  public LengthPrefixPageAndMdIterator(ByteRangeSupplier rangeSupplier) {
    this.rangeSupplier = rangeSupplier;
  }

  @Override
  public boolean hasNextPage() {
    return offset < rangeSupplier.getEnd();
  }

  @Override
  public byte[] readMetadata() throws RangeIterationException {
    // read 12 bytes from the supplier
    var lenBlock = rangeSupplier.getBytes(offset, 12);
    var bis = new ByteArrayInputStream(lenBlock);
    try {
      var b0 = OkapiIo.read(bis);
      var b1 = OkapiIo.read(bis);
      var b2 = OkapiIo.read(bis);
      var b3 = OkapiIo.read(bis);
      if (b0 != 'V' || b1 != '0' || b2 != '0' || b3 != '1') {
        throw new RangeIterationException();
      }
      mdLen = OkapiIo.readInt(bis);
      docBlockLen = OkapiIo.readInt(bis);
      return rangeSupplier.getBytes(offset, 12 + mdLen);
    } catch (IOException | StreamReadingException e) {
      throw new RangeIterationException(e);
    }
  }

  @Override
  public byte[] readPageBody() {
    var fromOff = offset + mdLen + 12;
    return rangeSupplier.getBytes(fromOff, docBlockLen);
  }

  @Override
  public void forward() {
    offset += (12 + mdLen + docBlockLen);
  }
}
