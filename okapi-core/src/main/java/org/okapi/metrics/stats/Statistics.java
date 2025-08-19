package org.okapi.metrics.stats;

import com.google.common.primitives.Ints;
import lombok.Getter;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.apache.datasketches.quantilescommon.QuantilesFloatsAPI;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Statistics {
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  @Getter private float sum;
  @Getter private float count;
  @Getter private float sumOfDeviationsSquared; // variance accumulator (optional)
  @Getter private QuantilesFloatsAPI floatsQuantiles;

  public Statistics(QuantilesFloatsAPI floatsQuantiles) {
    this.sum = 0f;
    this.count = 0f;
    this.sumOfDeviationsSquared = 0f;
    this.floatsQuantiles = floatsQuantiles;
  }

  public void update(MetricsContext ctx, float f) {
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

  public void update(MetricsContext ctx, float[] arr) {
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

  public float percentile(double quantile) {
    lock.readLock().lock();
    try {
      return floatsQuantiles.getQuantile(quantile);
    } finally {
      lock.readLock().unlock();
    }
  }

  public float avg() {
    lock.readLock().lock();
    try {
      return count == 0f ? 0f : (sum / count);
    } finally {
      lock.readLock().unlock();
    }
  }

  public float min() {
    lock.readLock().lock();
    try {
      return floatsQuantiles.getQuantile(0.0);
    } finally {
      lock.readLock().unlock();
    }
  }

  public float max() {
    lock.readLock().lock();
    try {
      return floatsQuantiles.getQuantile(1.0);
    } finally {
      lock.readLock().unlock();
    }
  }

  public byte[] serialize() {
    // todo: without this, KLLSketch flags get messed which makes serialization non-deterministic. Remove.
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

  public static Statistics deserialize(byte[] bytes, QuantileRestorer quantileRestorer) {
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

    QuantilesFloatsAPI quantiles;
    try {
      quantiles = quantileRestorer.restoreQuantiles(quantileBytes);
    } catch (Exception e) {
      throw new RuntimeException("Failed to restore quantiles", e);
    }

    var stats = new Statistics(quantiles);
    stats.sum = sum;
    stats.count = count;
    // sumOfDeviationsSquared cannot be restored from the legacy format; leave as 0
    return stats;
  }

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
}
