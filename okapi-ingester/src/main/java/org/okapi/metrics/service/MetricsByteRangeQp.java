/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.service;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.okapi.byterange.LengthPrefixedBlockSeekIterator;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.query.SeekIteratorQueryProcessor;
import org.okapi.primitives.ReadonlyHistogram;
import org.okapi.primitives.TimestampedReadonlySketch;
import org.okapi.s3.ByteRangeSupplier;

@AllArgsConstructor
public class MetricsByteRangeQp {

  MetricsPageCodec metricsPageCodec;

  public List<TimestampedReadonlySketch> getGauges(
      String name,
      Map<String, String> tags,
      RES_TYPE resType,
      long start,
      long end,
      ByteRangeSupplier byteRangeSupplier)
      throws Exception {
    var blockSeekIterator = new LengthPrefixedBlockSeekIterator(byteRangeSupplier);
    var qp = new SeekIteratorQueryProcessor(blockSeekIterator, metricsPageCodec);
    return qp.getGaugeSketches(name, tags, resType, start, end);
  }

  public List<ReadonlyHistogram> getHistograms(
      String name,
      Map<String, String> tags,
      long start,
      long end,
      ByteRangeSupplier byteRangeSupplier)
      throws Exception {
    var blockSeekIterator = new LengthPrefixedBlockSeekIterator(byteRangeSupplier);
    var qp = new SeekIteratorQueryProcessor(blockSeekIterator, metricsPageCodec);
    return qp.getHistograms(name, tags, start, end);
  }
}
