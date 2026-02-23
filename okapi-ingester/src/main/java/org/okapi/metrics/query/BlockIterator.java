package org.okapi.metrics.query;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.byterange.RangeIterationException;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;
import org.okapi.pages.BlockSeekIterator;
import org.okapi.pages.Codec;
import org.okapi.primitives.ChecksumedSerializable;

@Slf4j
@AllArgsConstructor
public class BlockIterator<T extends ChecksumedSerializable, M>
    implements ThrowableIterator<T, RangeIterationException> {
  String blockId;
  BlockSeekIterator seekIterator;
  OffsetFilter offsetFilter;
  MetadataFilter<M> metadataFilter;
  Supplier<T> blockSupplier;
  Codec<?, ?, M, ?> codec;

  @Override
  public boolean hasMore() {
    return seekIterator.hasNextPage();
  }

  @Override
  public Optional<T> next()
      throws RangeIterationException, StreamReadingException, IOException, NotEnoughBytesException {
    while (seekIterator.hasNextPage()) {
      var mdBytes = seekIterator.readMetadata();
      var decoded = codec.deserializeMetadata(mdBytes);

      if (decoded.isEmpty()) {
        log.error("Parsing error while reading metadata for: {}", blockId);
        seekIterator.forward();
        continue;
      }

      var md = decoded.get();

      if (!metadataFilter.shouldRead(md.metadata())) {
        seekIterator.forward();
        continue;
      }

      seekIterator.readOffsetTable();
      var offsetAndLenOpt = seekIterator.getOffsetAndLen(blockId);

      if (offsetAndLenOpt.isEmpty()) {
        seekIterator.forward();
        continue;
      }

      var offsetAndLen = offsetAndLenOpt.get();
      if (!offsetFilter.shouldRead(offsetAndLen)) {
        seekIterator.forward();
        continue;
      }

      var blockBytes = seekIterator.readBlock(offsetAndLen.getOffset(), offsetAndLen.getLen());
      var block = blockSupplier.get();
      block.fromChecksummedByteArray(blockBytes, 0, blockBytes.length);
      seekIterator.forward();
      return Optional.of(block);
    }
    return Optional.empty();
  }
}
