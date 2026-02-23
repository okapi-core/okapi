package org.okapi.metrics.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.OkapiBufferDecoder;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;
import org.okapi.pages.AbstractNonChecksummedCodec;

public class MetricsPageCodec
    extends AbstractNonChecksummedCodec<
        MetricsPage, MetricsPageSnapshot, MetricsPageMetadata, MetricsPageBody> {
  @Override
  public MetricsPage pageSupplier(MetricsPageMetadata metadata, MetricsPageBody body) {
    return new MetricsPage(metadata, body);
  }

  @Override
  public byte[] serializeMetadata(MetricsPageMetadata metadata) throws IOException {
    return metadata.toByteArray();
  }

  @Override
  public byte[] serializeBody(MetricsPageBody body) throws IOException {
    return body.toChecksummedByteArray();
  }

  @Override
  public Optional<LenBlockAndMetadata<MetricsPageMetadata>> deserializeMetadata(byte[] bytes)
      throws StreamReadingException, IOException, NotEnoughBytesException {
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
    var mdDecoder = new OkapiBufferDecoder();
    mdDecoder.setBuffer(bytes, 12, mdLen);
    if (!mdDecoder.isCrcMatch()) {
      return Optional.empty();
    }
    var md = new MetricsPageMetadata(1000, 0.01);
    md.fromDecoder(mdDecoder);
    return Optional.of(new LenBlockAndMetadata<>(mdLen, docBlockLen, md));
  }

  @Override
  public Optional<MetricsPageBody> deserializeBody(byte[] bytes, int offset, int length)
      throws StreamReadingException, IOException, NotEnoughBytesException {
    var page = new MetricsPageBody();
    page.fromChecksummedByteArray(bytes, offset, length);
    return Optional.of(page);
  }
}
