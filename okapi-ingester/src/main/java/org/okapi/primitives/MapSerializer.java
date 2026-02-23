/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.primitives;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.okapi.io.OkapiCheckedCountingWriter;
import org.okapi.protos.metrics.METRIC_TYPE;
import org.okapi.protos.metrics.OffsetAndLen;

public class MapSerializer {

  public record KeyValuePair(String key, ChecksumedSerializable value) {}

  public static Map<String, OffsetAndLen> serializeMap(
      int streamSize, Stream<KeyValuePair> map, METRIC_TYPE type, OkapiCheckedCountingWriter writer)
      throws IOException {
    var offsetTable = new HashMap<String, OffsetAndLen>();
    writer.writeByte((byte) 'B');
    writer.writeByte((byte) 'L');
    writer.writeByte((byte) 'O');
    writer.writeByte((byte) 'C');
    writer.writeInt(streamSize); // 4 bytes for size of the stream
    map.forEach(
        (kvp) -> {
          var block = kvp.value();
          byte[] blockBytes = null;
          try {
            blockBytes = block.toChecksummedByteArray();
            var off = writer.getTotalBytesWritten();
            writer.writeBytesWithLenPrefix(blockBytes);
            offsetTable.put(
                kvp.key(),
                OffsetAndLen.newBuilder()
                    .setMetricType(type)
                    .setOffset(off + 4) // skip the length prefix
                    .setLen(blockBytes.length)
                    .build());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
    return offsetTable;
  }
}
