package org.okapi.metrics.query;

import java.util.List;
import java.util.Map;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.primitives.ReadonlyHistogram;
import org.okapi.primitives.TimestampedReadonlySketch;

public interface MetricsClient {
  List<TimestampedReadonlySketch> queryGaugeSketches(
      String nodeId,
      String metricName,
      Map<String, String> paths,
      RES_TYPE resType,
      long qStart,
      long qEnd);

  List<ReadonlyHistogram> queryHistograms(
      String nodeId,
      String metricName,
      Map<String, String> paths,
      long qStart,
      long qEnd);
}
