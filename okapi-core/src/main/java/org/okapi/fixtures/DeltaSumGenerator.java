package org.okapi.fixtures;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.Getter;

public class DeltaSumGenerator {

  int maxCount;
  @Getter Duration windowSize;
  @Getter List<Sum> readings;
  public DeltaSumGenerator(int maxCount, Duration windowSize) {
    this.maxCount = maxCount;
    this.readings = new ArrayList<>();
    this.windowSize = windowSize;
  }

  public DeltaSumGenerator generate(long start, long end) {
    var random = new Random();
    for (var st = start; st < end; st += this.windowSize.toMillis()) {
      var count = random.nextInt(0, maxCount);
      readings.add(new Sum(count, st, st + this.windowSize.toMillis()));
    }
    return this;
  }

  public List<Sum> aggregate(long start, long end, Duration windowSize) {
    var sumList = new ArrayList<Sum>();
    for (long st = start; st < end; st += windowSize.toMillis()) {
      var intervalEnd = st + windowSize.toMillis();
      var sum = 0;
      for (var reading : readings) {
        if (reading.start() >= st && reading.end() <= intervalEnd) {
          sum += reading.count();
        }
      }
      sumList.add(new Sum(sum, st, intervalEnd));
    }
    return sumList;
  }

  public record Sum(int count, long start, long end) {}
}
