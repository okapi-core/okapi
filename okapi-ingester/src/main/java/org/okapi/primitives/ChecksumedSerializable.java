/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.primitives;

import java.io.IOException;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;

public interface ChecksumedSerializable {
  void fromChecksummedByteArray(byte[] bytes, int off, int len)
      throws StreamReadingException, IOException, NotEnoughBytesException;

  byte[] toChecksummedByteArray() throws IOException;
}
