package org.okapi.primitives;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.OkapiBufferDecoder;
import org.okapi.io.OkapiCheckedCountingWriter;
import org.okapi.protos.metrics.OffsetAndLen;

public class OffsetTableSerializer {

  public static byte[] serializeOffsetTable(Map<String, OffsetAndLen> offsetTable) throws IOException {
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeInt(offsetTable.size());
    for (var entry : offsetTable.entrySet()) {
      writer.writeBytesWithLenPrefix(entry.getKey().getBytes());
      var offsetAndLen = entry.getValue().toByteArray();
      writer.writeBytesWithLenPrefix(offsetAndLen);
    }
    writer.writeChecksum();
    return os.toByteArray();
  }

  public static Map<String, OffsetAndLen> deserializeOffsetTable(byte[] bytes, int off, int len)
      throws IOException, NotEnoughBytesException {
    var decoder = new OkapiBufferDecoder();
    decoder.setBuffer(bytes, off, len);
    if (!decoder.isCrcMatch()) {
      throw new IOException("Checksum mismatch when reading Offset Table");
    }
    var tableSize = decoder.nextInt();
    var offsetTable = new HashMap<String, OffsetAndLen>();
    for (int i = 0; i < tableSize; i++) {
      var pathBytes = decoder.nextBytesLenPrefix();
      var path = new String(pathBytes);
      var offsetAndLenBytes = decoder.nextBytesLenPrefix();
      var offsetAndLen = OffsetAndLen.parseFrom(offsetAndLenBytes);
      offsetTable.put(path, offsetAndLen);
    }
    return offsetTable;
  }
}
