/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.primitives;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import lombok.Locked;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.OkapiBufferDecoder;
import org.okapi.io.OkapiCheckedCountingWriter;
import org.okapi.io.StreamReadingException;
import org.okapi.primitives.ChecksumedSerializable;
import org.okapi.primitives.Histogram;
import org.okapi.primitives.ReadonlyHistogram;

public class HistoBlock implements ChecksumedSerializable {
  TreeMap<Long, Histogram> histograms = new TreeMap<>();

  @Locked
  public int updateHistogram(Long ts, Histogram histogram) {
    histograms.put(ts, histogram);
    return histogram.byteSize();
  }

  public Optional<ReadonlyHistogram> getHistogram(Long ts) {
    var hi = histograms.get(ts);
    if (hi == null) {
      return Optional.empty();
    }
    return Optional.of(
        new ReadonlyHistogram(
            hi.getStartTs(),
            hi.getEndTs(),
            hi.getTemporality(),
            hi.getBucketCounts(),
            hi.getBuckets()));
  }

  public List<ReadonlyHistogram> getAllInRange(Long st, Long en) {
    var subMap = histograms.subMap(st, true, en, true);
    return subMap.values().stream()
        .map(
            hi ->
                new ReadonlyHistogram(
                    hi.getStartTs(),
                    hi.getEndTs(),
                    hi.getTemporality(),
                    hi.getBucketCounts(),
                    hi.getBuckets()))
        .toList();
  }

  @Override
  public void fromChecksummedByteArray(byte[] bytes, int off, int len)
      throws StreamReadingException, IOException, NotEnoughBytesException {
    var decoder = new OkapiBufferDecoder();
    decoder.setBuffer(bytes, off, len);
    if (!decoder.isCrcMatch()) {
      throw new StreamReadingException("CRC mismatch when reading HistoBlock");
    }
    byte[] flags = decoder.nextBytesNoLenPrefix(2);
    if (flags[0] != BlockFlags.HISTO[0] || flags[1] != BlockFlags.HISTO[1]) {
      throw new StreamReadingException("Invalid block flags when reading HistoBlock");
    }
    var histoSize = decoder.nextInt();
    for (int i = 0; i < histoSize; i++) {
      var key = decoder.nextLong();
      var valueBytes = decoder.nextBytesLenPrefix();
      var histogram = new Histogram();
      histogram.fromByteArray(valueBytes, 0, valueBytes.length);
      histograms.put(key, histogram);
    }
  }

  @Override
  public byte[] toChecksummedByteArray() throws IOException {
    var os = new ByteArrayOutputStream();
    var writer = new OkapiCheckedCountingWriter(os);
    writer.writeBytesWithoutLenPrefix(BlockFlags.HISTO);
    writer.writeInt(histograms.size());
    for (var entry : histograms.entrySet()) {
      writer.writeLong(entry.getKey());
      writer.writeBytesWithLenPrefix(entry.getValue().toByteArray());
    }
    writer.writeChecksum();
    return os.toByteArray();
  }
}
