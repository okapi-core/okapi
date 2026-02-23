package org.okapi.metrics.query;

import java.util.List;
import java.util.Map;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.primitives.ReadonlyHistogram;
import org.okapi.primitives.TimestampedReadonlySketch;

public interface MetricsQueryProcessor {
  List<TimestampedReadonlySketch> getGaugeSketches(
      String metricName, Map<String, String> paths, RES_TYPE resType, long startTime, long endTime)
      throws Exception;

  List<ReadonlyHistogram> getHistograms(
      String metricName, Map<String, String> paths, long startTime, long endTime)
      throws Exception;
}
