/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;

@Slf4j
public abstract class AbstractNonChecksummedCodec<P extends AppendOnlyPage<?, S, M, B>, S, M, B>
    implements Codec<P, S, M, B> {

  public abstract P pageSupplier(M metadata, B body);

  @Override
  public final byte[] serialize(P page) throws IOException {
    byte[] md = serializeMetadata(page.getMetadata());
    byte[] docBlock = serializeBody(page.getPageBody());
    // write page sections
    ByteArrayOutputStream pageStream = new ByteArrayOutputStream();
    OkapiIo.write(pageStream, (byte) 'V');
    OkapiIo.write(pageStream, (byte) '0');
    OkapiIo.write(pageStream, (byte) '0');
    OkapiIo.write(pageStream, (byte) '1');
    OkapiIo.writeInt(pageStream, md.length);
    OkapiIo.writeInt(pageStream, docBlock.length);
    OkapiIo.writeBytesWithoutLenPrefix(pageStream, md);
    OkapiIo.writeBytesWithoutLenPrefix(pageStream, docBlock);
    return pageStream.toByteArray();
  }

  @Override
  public final Optional<P> deserialize(byte[] bytes)
      throws IOException, StreamReadingException, NotEnoughBytesException {
    var maybeMd = deserializeMetadata(bytes);
    if (maybeMd.isEmpty()) {
      return Optional.empty();
    }
    var md = maybeMd.get();
    var body = deserializeBody(bytes, 12 + md.mdLen(), md.docBlockLen());
    return body.map(b -> pageSupplier(md.metadata(), b));
  }
}
