package org.okapi.metrics.stats;

import com.google.common.primitives.Ints;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Getter;
import org.apache.datasketches.kll.KllFloatsSketch;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.pojos.AGG_TYPE;

public class RolledUpStatistics implements UpdatableStatistics {
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  @Getter private float sum;
  @Getter private float count;
  @Getter private float sumOfDeviationsSquared; // variance accumulator (optional)
  private KllFloatsSketch floatsQuantiles;
  private final AtomicBoolean frozen = new AtomicBoolean(false);

  public RolledUpStatistics(KllFloatsSketch floatsQuantiles) {
    this.sum = 0f;
    this.count = 0f;
    this.sumOfDeviationsSquared = 0f;
    this.floatsQuantiles = floatsQuantiles;
  }

  protected RolledUpStatistics(float sum, float count, KllFloatsSketch quantilesFloatsAPI) {
    this.sum = sum;
    this.count = count;
    this.floatsQuantiles = quantilesFloatsAPI;
    this.frozen.set(true);
  }

  public static RolledUpStatistics deserialize(byte[] bytes, QuantileRestorer quantileRestorer) {

    if (bytes.length < 8) {
      throw new IllegalArgumentException("Not enough bytes to deserialize Statistics");
    }
    var sumBytes = new byte[4];
    var countBytes = new byte[4];
    System.arraycopy(bytes, 0, sumBytes, 0, 4);
    System.arraycopy(bytes, 4, countBytes, 0, 4);

    float sum = Float.intBitsToFloat(Ints.fromByteArray(sumBytes));
    float count = Float.intBitsToFloat(Ints.fromByteArray(countBytes));
    var quantileBytes = new byte[bytes.length - 8];
    System.arraycopy(bytes, 8, quantileBytes, 0, quantileBytes.length);

    KllFloatsSketch quantiles;
    try {
      quantiles = quantileRestorer.restoreQuantiles(quantileBytes);
    } catch (Exception e) {
      throw new RuntimeException("Failed to restore quantiles", e);
    }

    var stats = new RolledUpStatistics(quantiles);
    stats.sum = sum;
    stats.count = count;
    // sumOfDeviationsSquared cannot be restored from the legacy format; leave as 0
    return stats;
  }

  @Override
  public void update(MetricsContext ctx, float f) throws StatisticsFrozenException {

    if (this.frozen.get()) {
      throw new StatisticsFrozenException();
    }

    lock.writeLock().lock();
    try {
      sum += f;
      var mean = sum / count;
      var deviation = f - mean;
      sumOfDeviationsSquared += deviation * deviation;
      count++;
      floatsQuantiles.update(f);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void update(MetricsContext ctx, float[] arr) throws StatisticsFrozenException {
    if (this.frozen.get()) {
      throw new StatisticsFrozenException();
    }

    lock.writeLock().lock();
    try {
      for (int i = 0; i < arr.length; i++) {
        // route through same logic, but we’re already under the write lock
        final float x = arr[i];
        final float prevCount = count;
        final float prevSum = sum;

        if (prevCount == 0f) {
          sum = x;
          count = 1f;
          sumOfDeviationsSquared = 0f;
        } else {
          final float prevMean = prevSum / prevCount;
          sum = prevSum + x;
          final float newCount = prevCount + 1f;
          final float newMean = sum / newCount;
          sumOfDeviationsSquared += (x - prevMean) * (x - newMean);
          count = newCount;
        }
        floatsQuantiles.update(x);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public float percentile(double quantile) {
    return floatsQuantiles.getQuantile(quantile);
  }

  @Override
  public float avg() {
    return count == 0f ? 0f : (sum / count);
  }

  @Override
  public float min() {
    return floatsQuantiles.getQuantile(0.0);
  }

  @Override
  public float max() {
    return floatsQuantiles.getQuantile(1.0);
  }

  @Override
  public byte[] serialize() {
    // todo: without this, KLLSketch flags get messed which makes serialization non-deterministic.
    min();
    lock.readLock().lock();
    try {
      var sumAsInt = Ints.toByteArray(Float.floatToIntBits(sum));
      var countAsInt = Ints.toByteArray(Float.floatToIntBits(count));
      var quantileBytes = floatsQuantiles.toByteArray();

      var bytes = new byte[4 + 4 + quantileBytes.length];
      System.arraycopy(sumAsInt, 0, bytes, 0, 4);
      System.arraycopy(countAsInt, 0, bytes, 4, 4);
      System.arraycopy(quantileBytes, 0, bytes, 8, quantileBytes.length);
      return bytes;
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public float aggregate(AGG_TYPE aggType) {
    lock.readLock().lock();
    try {
      return switch (aggType) {
        case SUM -> sum;
        case COUNT -> count;
        case AVG -> count == 0f ? 0f : (sum / count);
        case MIN -> floatsQuantiles.getQuantile(0.0);
        case MAX -> floatsQuantiles.getQuantile(1.0);
        case P50 -> floatsQuantiles.getQuantile(0.5);
        case P75 -> floatsQuantiles.getQuantile(0.75);
        case P90 -> floatsQuantiles.getQuantile(0.9);
        case P95 -> floatsQuantiles.getQuantile(0.95);
        case P99 -> floatsQuantiles.getQuantile(0.99);
      };
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean freeze() {
    lock.writeLock().lock();
    frozen.set(true);
    lock.writeLock().unlock();
    return frozen.get();
  }

  protected KllFloatsSketch getSketch() {
    return this.floatsQuantiles;
  }
}
