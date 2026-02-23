package org.okapi.primitives;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import org.apache.datasketches.kll.KllFloatsSketch;
import org.okapi.metrics.stats.KllSketchRestorer;

public class ReadOnlySketch {

  @Getter private final float mean;
  @Getter private final float count;
  @Getter private final float sumOfDeviationsSquared;
  private final KllFloatsSketch floatsSketch;

  public float getQuantile(double rank) {
    return floatsSketch.getQuantile(rank);
  }

  @VisibleForTesting
  public KllFloatsSketch getFloatsSketch() {
    return floatsSketch;
  }

  public ReadOnlySketch(
      float mean, float count, float sumOfDeviationsSquared, byte[] floatsSketchBytes) {
    this.mean = mean;
    this.count = count;
    this.sumOfDeviationsSquared = sumOfDeviationsSquared;
    this.floatsSketch = KllSketchRestorer.restoreFromBytes(floatsSketchBytes);
  }

  public void mergeInto(KllFloatsSketch sketch) {
    sketch.merge(this.floatsSketch);
  }
}
