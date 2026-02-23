/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.io;

import java.util.Optional;

public interface CheckedBufferDecoder {
  DECODER_STATE getState();

  boolean isCrcMatch();

  Optional<Integer> getExpectedCrc();

  Optional<Integer> getComputedCrc();

  void setBuffer(byte[] bytes, int off, int len);

  int nextInt() throws NotEnoughBytesException;

  long nextLong() throws NotEnoughBytesException;

  byte[] nextBytesLenPrefix() throws NotEnoughBytesException;

  byte[] nextBytesNoLenPrefix(int n) throws NotEnoughBytesException;

  enum DECODER_STATE {
    CREATED,
    VALIDATED,
    INIT,
    INVALID_BUFFER,
    BUFFER_FULLY_READ
  }
}
