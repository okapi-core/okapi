package org.okapi.fixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.Getter;

public class CsumGenerator {
  public record Csum(long start, long end, int sum) {}

  int maxCount;
  @Getter List<Csum> readings;

  public CsumGenerator(int maxCount) {
    this.maxCount = maxCount;
    this.readings = new ArrayList<>();
  }

  public void generate(long start, long end) {
    var random = new Random();
    var count = random.nextInt(0, maxCount);
    this.readings.add(new Csum(start, end, count));
  }

  public Csum aggregate(long start, long end) {
    // largest overlapping interval with the given interval and its value
    var overlapSize = -1;
    var overlapValue = -1;
    for (var reading : readings) {
      if (reading.start() >= start && reading.end() <= end) {
        var size = reading.end() - reading.start();
        if (size > overlapSize) {
          overlapValue = reading.sum();
        }
      }
    }
    return new Csum(start, end, overlapValue);
  }
}
