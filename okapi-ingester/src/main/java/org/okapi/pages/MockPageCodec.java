/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;

public class MockPageCodec
    extends AbstractNonChecksummedCodec<
        MockAppendPage, MockPageSnapshot, MockPageMetadata, MockPageBody> {
  Gson gson = new Gson();

  @Override
  public MockAppendPage pageSupplier(MockPageMetadata metadata, MockPageBody body) {
    return new MockAppendPage(metadata, body);
  }

  @Override
  public byte[] serializeMetadata(MockPageMetadata metadata) throws IOException {
    Preconditions.checkNotNull(metadata);
    return gson.toJson(metadata).getBytes();
  }

  @Override
  public byte[] serializeBody(MockPageBody body) throws IOException {
    Preconditions.checkNotNull(body);
    return gson.toJson(body).getBytes();
  }

  @Override
  public Optional<LenBlockAndMetadata<MockPageMetadata>> deserializeMetadata(byte[] bytes)
      throws StreamReadingException, IOException {
    var bis = new ByteArrayInputStream(bytes);
    var b0 = OkapiIo.read(bis);
    var b1 = OkapiIo.read(bis);
    var b2 = OkapiIo.read(bis);
    var b3 = OkapiIo.read(bis);
    if (b0 != 'V' || b1 != '0' || b2 != '0' || b3 != '1') {
      throw new RuntimeException("Mismatching versions.");
    }
    var mdLen = OkapiIo.readInt(bis);
    var docBlockLen = OkapiIo.readInt(bis);
    var str = OkapiIo.readNBytes(bis, mdLen);
    var meta = gson.fromJson(new String(str), MockPageMetadata.class);
    return Optional.of(new LenBlockAndMetadata<>(mdLen, docBlockLen, meta));
  }

  @Override
  public Optional<MockPageBody> deserializeBody(byte[] bytes, int offset, int length) {
    var buf = new byte[length];
    System.arraycopy(bytes, offset, buf, 0, length);
    var deserialized = gson.fromJson(new String(buf), MockPageBody.class);
    return Optional.of(deserialized);
  }
}
