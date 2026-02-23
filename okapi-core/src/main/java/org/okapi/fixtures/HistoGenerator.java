/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.fixtures;

import com.google.common.primitives.Ints;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.Getter;

public class HistoGenerator {
  List<Float> buckets;
  int maxCount;
  @Getter List<Histo> readings;
  Duration window;

  public HistoGenerator(List<Float> buckets, int maxCount, Duration window) {
    this.buckets = buckets;
    this.maxCount = maxCount;
    this.readings = new ArrayList<>();
    this.window = window;
  }

  private void generateSingle(long start, long end) {
    int n = 1 + buckets.size();
    var counts = new ArrayList<Integer>();
    var random = new Random();
    for (int i = 0; i < n; i++) {
      var count = random.nextInt(0, maxCount);
      counts.add(count);
    }
    readings.add(new Histo(counts, start, end));
  }

  public void generate(long start, long end) {
    // generate multiple rolling histograms
    for (var st = start; st < end; st += this.window.toMillis()) {
      generateSingle(st, st + this.window.toMillis());
    }
  }

  public Histo aggregate(long start, long end) {
    var counts = new int[this.buckets.size() + 1];
    for (var reading : readings) {
      if (reading.start() >= start && reading.end() <= end) {
        for (int i = 0; i < reading.counts().size(); i++) {
          counts[i] += reading.counts().get(i);
        }
      }
    }
    return new Histo(Ints.asList(counts), start, end);
  }

  public record Histo(List<Integer> counts, long start, long end) {}
}
