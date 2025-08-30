package org.okapi.promql;

import java.util.List;
import lombok.Getter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.okapi.Statistics;

public class MockStatistics implements Statistics {

  @Getter
  List<Float> values;
  DescriptiveStatistics statistics;

  public MockStatistics(List<Float> values) {
    this.values = values;
    this.statistics = new DescriptiveStatistics();
    for (var f : this.values) {
      statistics.addValue(f);
    }
  }

  @Override
  public float percentile(double quantile) {
    return (float) statistics.getPercentile(100 * quantile);
  }

  @Override
  public float avg() {
    return (float) statistics.getMean();
  }

  @Override
  public float min() {
    return (float) statistics.getMin();
  }

  @Override
  public float max() {
    return (float) statistics.getMax();
  }

  @Override
  public byte[] serialize() {
    return new byte[0];
  }

  @Override
  public float getSum() {
    return (float) statistics.getSum();
  }

  @Override
  public float getCount() {
    return 0;
  }

  @Override
  public float getSumOfDeviationsSquared() {
    return (float) statistics.getVariance();
  }
}
