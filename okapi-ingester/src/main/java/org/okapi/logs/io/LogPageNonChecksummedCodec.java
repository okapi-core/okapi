package org.okapi.logs.io;

import static org.okapi.serialization.BloomSerializer.*;

import com.github.luben.zstd.Zstd;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import org.okapi.io.*;
import org.okapi.pages.AbstractNonChecksummedCodec;
import org.okapi.primitives.BinaryLogRecordV1;

public final class LogPageNonChecksummedCodec
    extends AbstractNonChecksummedCodec<LogPage, LogPageSnapshot, LogPageMetadata, LogPageBody> {

  @Override
  public LogPage pageSupplier(LogPageMetadata metadata, LogPageBody body) {
    return new LogPage(metadata, body);
  }

  @Override
  public byte[] serializeMetadata(LogPageMetadata metadata) throws IOException {
    byte[] levelSet = serializeBloom(metadata.getLogLevels());
    byte[] traceSet = serializeBloom(metadata.getTraceIdSet());
    byte[] tri = serializeBloom(metadata.getLogBodyTrigrams());
    byte[] md;
    {
      var mdStream = new ByteArrayOutputStream();
      var mdWriter = new OkapiCheckedCountingWriter(mdStream);
      mdWriter.writeLong(metadata.getTsStart());
      mdWriter.writeLong(metadata.getTsEnd());
      mdWriter.writeBytesWithLenPrefix(levelSet);
      mdWriter.writeBytesWithLenPrefix(traceSet);
      mdWriter.writeBytesWithLenPrefix(tri);
      mdWriter.writeChecksum();
      md = mdStream.toByteArray();
    }
    return md;
  }

  @Override
  public byte[] serializeBody(LogPageBody body) throws IOException {
    var snap = body.toSnapshot();
    var docStream = new ByteArrayOutputStream();
    var docsWriter = new OkapiCheckedCountingWriter(docStream);
    docsWriter.writeInt(snap.getLogDocs().size());
    var compressedBuf = new ByteArrayOutputStream();
    for (var doc : snap.getLogDocs()) {
      OkapiIo.writeBytes(compressedBuf, doc.toByteArray());
    }
    docsWriter.writeBytesWithLenPrefix(Zstd.compress(compressedBuf.toByteArray()));
    docsWriter.writeChecksum();
    return docStream.toByteArray();
  }

  @Override
  public Optional<LenBlockAndMetadata<LogPageMetadata>> deserializeMetadata(byte[] bytes)
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
    // read the filters,
    var tsStart = mdDecoder.nextLong();
    var tsEnd = mdDecoder.nextLong();
    var levelSet = deserializeIntBloom(mdDecoder.nextBytesLenPrefix());
    var traceSet = deserializeStringBloom(mdDecoder.nextBytesLenPrefix());
    var trigramSet = deserializeIntBloom(mdDecoder.nextBytesLenPrefix());
    var metadata = new LogPageMetadata(tsStart, tsEnd, levelSet, traceSet, trigramSet);
    return Optional.of(new LenBlockAndMetadata<>(mdLen, docBlockLen, metadata));
  }

  @Override
  public Optional<LogPageBody> deserializeBody(byte[] bytes, int offset, int length)
      throws StreamReadingException, IOException, NotEnoughBytesException {
    var pageDecoder = new OkapiBufferDecoder();
    pageDecoder.setBuffer(bytes, offset, length);
    if (!pageDecoder.isCrcMatch()) {
      return Optional.empty();
    }

    // read the docs
    var nDocs = pageDecoder.nextInt();
    var docs = new ArrayList<BinaryLogRecordV1>(nDocs);
    var compressed = pageDecoder.nextBytesLenPrefix();
    var decompressed = new ByteArrayInputStream(Zstd.decompress(compressed));
    for (int i = 0; i < nDocs; i++) {
      docs.add(BinaryLogRecordV1.fromByteArray(OkapiIo.readBytes(decompressed)));
    }

    return Optional.of(new LogPageBody(docs));
  }
}
