package org.okapi.traces.io;

import static org.okapi.serialization.BloomSerializer.*;

import com.github.luben.zstd.Zstd;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import org.okapi.io.*;
import org.okapi.pages.AbstractNonChecksummedCodec;
import org.okapi.primitives.BinarySpanRecordV2;

public final class SpanPageCodec
    extends AbstractNonChecksummedCodec<
        SpanPage, SpanPageSnapshot, SpanPageMetadata, SpanPageBody> {

  @Override
  public SpanPage pageSupplier(SpanPageMetadata metadata, SpanPageBody body) {
    return new SpanPage(metadata, body);
  }

  @Override
  public byte[] serializeMetadata(SpanPageMetadata metadata) throws IOException {
    byte[] traceIdBloom = serializeBloom(metadata.getTraceIdFilter());
    byte[] spanBloom = serializeBloom(metadata.getSpanIdFilter());
    byte[] md;
    {
      var mdStream = new ByteArrayOutputStream();
      OkapiIo.writeLong(mdStream, metadata.getTsStart());
      OkapiIo.writeLong(mdStream, metadata.getTsEnd());
      OkapiIo.writeBytes(mdStream, traceIdBloom);
      OkapiIo.writeBytes(mdStream, spanBloom);
      md = mdStream.toByteArray();
    }
    return md;
  }

  @Override
  public Optional<LenBlockAndMetadata<SpanPageMetadata>> deserializeMetadata(byte[] bytes)
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
    var tsStart = OkapiIo.readLong(bis);
    var tsEnd = OkapiIo.readLong(bis);
    var traceSet = deserializeByteArrayBloom(OkapiIo.readBytes(bis));
    var spanSet = deserializeByteArrayBloom(OkapiIo.readBytes(bis));
    return Optional.of(
        new LenBlockAndMetadata<>(
            mdLen, docBlockLen, new SpanPageMetadata(tsStart, tsEnd, traceSet, spanSet)));
  }

  @Override
  public byte[] serializeBody(SpanPageBody body) throws IOException {
    var docStream = new ByteArrayOutputStream();
    var docsWriter = new OkapiCheckedCountingWriter(docStream);
    var snap = body.toSnapshot();
    docsWriter.writeInt(snap.getSpans().size());
    var compressedBuf = new ByteArrayOutputStream();
    for (var payload : snap.getSpans()) {
      OkapiIo.writeBytes(compressedBuf, payload.toByteArray());
    }
    docsWriter.writeBytesWithLenPrefix(Zstd.compress(compressedBuf.toByteArray()));
    return docStream.toByteArray();
  }

  @Override
  public Optional<SpanPageBody> deserializeBody(byte[] bytes, int offset, int length)
      throws StreamReadingException, IOException, NotEnoughBytesException {
    var pageDecoder = new OkapiBufferDecoder();
    pageDecoder.setBuffer(bytes, offset, length);
    var nDocs = pageDecoder.nextInt();
    var docs = new ArrayList<BinarySpanRecordV2>(nDocs);
    var compressed = pageDecoder.nextBytesLenPrefix();
    var decompressed = new ByteArrayInputStream(Zstd.decompress(compressed));
    for (int i = 0; i < nDocs; i++) {
      docs.add(BinarySpanRecordV2.fromByteArray(OkapiIo.readBytes(decompressed)));
    }
    return Optional.of(new SpanPageBody(docs));
  }
}
