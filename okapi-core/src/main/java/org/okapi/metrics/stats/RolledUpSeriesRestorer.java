/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.stats;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Function;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.rollup.RollupSeries;

public class RolledUpSeriesRestorer implements RollupSeriesRestorer<UpdatableStatistics> {

  private final Function<Integer, RollupSeries<UpdatableStatistics>> seriesSupplier;

  public RolledUpSeriesRestorer(
      Function<Integer, RollupSeries<UpdatableStatistics>> seriesSupplier) {
    this.seriesSupplier = seriesSupplier;
  }

  @Override
  public RollupSeries<UpdatableStatistics> restore(int shard, InputStream is)
      throws StreamReadingException, IOException {
    var series = seriesSupplier.apply(shard);
    return series;
  }

  public RollupSeries<UpdatableStatistics> restore(int shard, Path checkpointPath)
      throws IOException, StreamReadingException {
    try (var fis = new FileInputStream(checkpointPath.toFile())) {
      return restore(shard, fis);
    }
  }
}
