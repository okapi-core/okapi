/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.primitives;

import java.io.IOException;
import org.okapi.io.StreamReadingException;

public interface RawSerializable {
  byte[] toByteArray() throws IOException;

  void fromByteArray(byte[] bytes, int offset, int len) throws StreamReadingException, IOException;

  int byteSize();
}
