package org.okapi.traces.sampler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HeadSamplingStrategy implements SamplingStrategy {

  private final double fraction;

  public HeadSamplingStrategy(@Value("${sampling.fraction:1.0}") double fraction) {
    this.fraction = fraction;
  }

  @Override
  public boolean sample(String traceId) {
    // Compute a normalized hash value in [0,1)
    int hash = Math.abs(traceId.hashCode());
    double normalized = (double) hash / Integer.MAX_VALUE;
    return normalized < fraction;
  }
}
