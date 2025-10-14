package org.okapi.promql.eval;

import java.util.Map;
import org.okapi.metrics.pojos.results.Scan;

public class VectorData {
  public record Labels(Map<String, String> tags) {}

  public record SeriesId(String metric, Labels labels) {}

  public record Sample(long ts, float value) {}

  public record SeriesSample(SeriesId id, Sample sample) {
    public SeriesId series() {
      return id;
    }
  }

  // A window now carries the scan for the series over the requested range
  public record SeriesWindow(SeriesId id, Scan scan) {}
}
