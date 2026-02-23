/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import java.io.IOException;
import java.util.Optional;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;

/** Codec for serializing/deserializing append-only pages. */
public interface Codec<P extends AppendOnlyPage<?, S, M, B>, S, M, B> {

  byte[] serialize(P page) throws java.io.IOException;

  Optional<P> deserialize(byte[] bytes)
      throws java.io.IOException, StreamReadingException, NotEnoughBytesException;

  byte[] serializeMetadata(M metadata) throws IOException;

  byte[] serializeBody(B body) throws IOException;

  Optional<LenBlockAndMetadata<M>> deserializeMetadata(byte[] bytes)
      throws StreamReadingException, IOException, NotEnoughBytesException;

  Optional<B> deserializeBody(byte[] bytes, int offset, int length)
      throws StreamReadingException, IOException, NotEnoughBytesException;

  record LenBlockAndMetadata<M>(int mdLen, int docBlockLen, M metadata) {}
}
