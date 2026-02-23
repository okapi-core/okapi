package org.okapi.byterange;

import com.google.common.primitives.Ints;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.pages.BlockSeekIterator;
import org.okapi.primitives.OffsetTableSerializer;
import org.okapi.protos.metrics.OffsetAndLen;
import org.okapi.s3.ByteRangeSupplier;

@Slf4j
public class LengthPrefixedBlockSeekIterator extends LengthPrefixPageAndMdIterator
    implements BlockSeekIterator {
  ByteRangeSupplier rangeSupplier;
  Map<String, OffsetAndLen> offsetTable;
  long blockStreamStart = -1;

  public LengthPrefixedBlockSeekIterator(ByteRangeSupplier rangeSupplier) {
    super(rangeSupplier);
    offsetTable = new HashMap<>();
    this.rangeSupplier = rangeSupplier;
  }

  @Override
  public byte[] readBlock(long offset, int length) {
    var byteRangeStart = blockStreamStart + offset;
    return rangeSupplier.getBytes(byteRangeStart, length);
  }

  @Override
  public void readOffsetTable() throws IOException, NotEnoughBytesException {
    // at body start -> first 4 bytes are length of offset table serialized
    var bodyStart = offset + mdLen + 12;
    var lenBytes = rangeSupplier.getBytes(bodyStart, 4);
    var tableLen = Ints.fromByteArray(lenBytes);
    var tableBytes = rangeSupplier.getBytes(bodyStart + 4, tableLen);
    var offsetTable =
        OffsetTableSerializer.deserializeOffsetTable(tableBytes, 0, tableBytes.length);
    this.offsetTable = offsetTable;
    this.blockStreamStart = bodyStart + 4 + tableLen + 4;
  }

  @Override
  public Optional<OffsetAndLen> getOffsetAndLen(String blockId) {
    return Optional.ofNullable(offsetTable.get(blockId));
  }

  @Override
  public boolean hasBlock(String blockId) {
    return offsetTable.containsKey(blockId);
  }
}
