/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.intervals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IntervalUtils {

  /**
   * Returns the intersection of [start, end) with (-INF, ref + 1) If there's no intersection,
   * returns Optional.empty().
   */
  public static Optional<Interval> clipBefore(long start, long end, long ref) {
    long newEnd = Math.min(end, ref + 1);
    if (start >= newEnd) {
      return Optional.empty();
    }
    return Optional.of(new Interval(start, newEnd));
  }

  /**
   * Returns the intersection of [start, end) with [ref, +INF) If there's no intersection, returns
   * Optional.empty().
   */
  public static Optional<Interval> clipAfter(long start, long end, long ref) {
    long newStart = Math.max(start, ref);
    if (newStart >= end) {
      return Optional.empty();
    }
    return Optional.of(new Interval(newStart, end));
  }

  public static boolean isOverlapping(long start1, long end1, long start2, long end2) {
    return start1 < end2 && start2 < end1;
  }

  public static List<long[]> merge(List<long[]> intervals) {
    var merged = new ArrayList<long[]>();
    if (intervals.isEmpty()) return merged;
    var st = intervals.get(0)[0];
    var en = intervals.get(0)[1];
    for (var interval : intervals.subList(1, intervals.size())) {
      if (interval[0] == en) {
        en = interval[1];
      } else {
        merged.add(new long[] {st, en});
        st = interval[0];
        en = interval[1];
      }
    }
    merged.add(new long[] {st, en});
    return merged;
  }

  public static long alignedStart(long end, long alignTo) {
    var alignedStart = alignTo * (end / alignTo);
    return alignedStart;
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
