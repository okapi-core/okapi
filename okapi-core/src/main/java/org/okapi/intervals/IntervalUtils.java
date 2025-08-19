package org.okapi.intervals;

import java.util.Optional;

public class IntervalUtils {

  /**
   * Returns the intersection of [start, end) with (-INF, ref + 1)
   * If there's no intersection, returns Optional.empty().
   */
  public static Optional<Interval> clipBefore(long start, long end, long ref) {
    long newEnd = Math.min(end, ref + 1);
    if (start >= newEnd) {
      return Optional.empty();
    }
    return Optional.of(new Interval(start, newEnd));
  }

  /**
   * Returns the intersection of [start, end) with [ref, +INF)
   * If there's no intersection, returns Optional.empty().
   */
  public static Optional<Interval> clipAfter(long start, long end, long ref) {
    long newStart = Math.max(start, ref);
    if (newStart >= end) {
      return Optional.empty();
    }
    return Optional.of(new Interval(newStart, end));
  }

  public record Interval(long start, long end) {
    public long length() {
      return end - start;
    }

    public boolean isEmpty() {
      return start >= end;
    }

    @Override
    public String toString() {
      return "[" + start + ", " + end + ")";
    }
  }
}
