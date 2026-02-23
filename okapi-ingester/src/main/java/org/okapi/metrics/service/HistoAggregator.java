package org.okapi.metrics.service;

import java.util.List;
import org.okapi.primitives.ReadonlyHistogram;
import org.okapi.rest.metrics.query.GetHistogramResponse;
import org.okapi.rest.metrics.query.Histogram;

public class HistoAggregator {
  public static final GetHistogramResponse aggregateHistograms(List<ReadonlyHistogram> histograms) {
    var histos =
        histograms.stream()
            .map(
                h ->
                    Histogram.builder()
                        .buckets(h.getBuckets())
                        .counts(h.getBucketCounts())
                        .start(h.getStartTs())
                        .end(h.getEndTs())
                        .build())
            .toList();
    return GetHistogramResponse.builder().histograms(histos).build();
  }
}
