package org.okapi.metrics.io;

import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.okapi.metrics.primitives.GaugeBlock;
import org.okapi.metrics.primitives.HistoBlock;
import org.okapi.primitives.ReadOnlySketch;
import org.okapi.primitives.ReadonlyHistogram;

@AllArgsConstructor
public class MetricsPageBodySnapshot {
  Map<String, GaugeBlock> gauges;
  Map<String, HistoBlock> histos;

  public Optional<ReadOnlySketch> getSecondly(String path, Long ts, double[] ranks) {
    var gaugeBlock = gauges.get(path);
    return Optional.ofNullable(gaugeBlock.getSecondlyStat(ts / 1000, ranks));
  }

  public Optional<ReadOnlySketch> getMinutely(String path, Long ts, double[] ranks) {
    var gaugeBlock = gauges.get(path);
    return Optional.ofNullable(gaugeBlock.getMinutelyStat(ts / 60000, ranks));
  }

  public Optional<ReadOnlySketch> getHourly(String path, Long ts, double[] ranks) {
    var gaugeBlock = gauges.get(path);
    return Optional.ofNullable(gaugeBlock.getHourlyStat(ts / 3600000, ranks));
  }

  public Optional<ReadonlyHistogram> getHistogram(String path, Long ts) {
    var histoBlock = histos.get(path);
    if (histoBlock == null) {
      return Optional.empty();
    }
    return histoBlock.getHistogram(ts);
  }
}
