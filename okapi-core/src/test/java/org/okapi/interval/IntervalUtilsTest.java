package org.okapi.interval;

import org.okapi.intervals.IntervalUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class IntervalUtilsTest {

  static Stream<Arguments> clipBeforeCases() {
    return Stream.of(
        Arguments.of(10, 20, 15, Optional.of(new IntervalUtils.Interval(10, 16))),
        Arguments.of(10, 20, 25, Optional.of(new IntervalUtils.Interval(10, 20))),
        Arguments.of(10, 20, 5, Optional.empty()),
        Arguments.of(10, 20, 9, Optional.empty()),
        Arguments.of(10, 20, 19, Optional.of(new IntervalUtils.Interval(10, 20))));
  }

  @ParameterizedTest
  @MethodSource("clipBeforeCases")
  void testClipBefore(long start, long end, long ref, Optional<IntervalUtils.Interval> expected) {
    Optional<IntervalUtils.Interval> result = IntervalUtils.clipBefore(start, end, ref);
    assertEquals(expected, result);
  }

  static Stream<Arguments> clipAfterCases() {
    return Stream.of(
        Arguments.of(10, 20, 15, Optional.of(new IntervalUtils.Interval(15, 20))),
        Arguments.of(10, 20, 5, Optional.of(new IntervalUtils.Interval(10, 20))),
        Arguments.of(10, 20, 25, Optional.empty()),
        Arguments.of(10, 20, 20, Optional.empty()),
        Arguments.of(10, 20, 10, Optional.of(new IntervalUtils.Interval(10, 20))));
  }

  @ParameterizedTest
  @MethodSource("clipAfterCases")
  void testClipAfter(long start, long end, long ref, Optional<IntervalUtils.Interval> expected) {
    Optional<IntervalUtils.Interval> result = IntervalUtils.clipAfter(start, end, ref);
    assertEquals(expected, result);
  }

  @Test
  void testIsEmpty() {
    assertTrue(new IntervalUtils.Interval(5, 5).isEmpty());
    assertTrue(new IntervalUtils.Interval(8, 3).isEmpty());
    assertFalse(new IntervalUtils.Interval(3, 8).isEmpty());
  }

  @Test
  void testLength() {
    assertEquals(5, new IntervalUtils.Interval(3, 8).length());
    assertEquals(0, new IntervalUtils.Interval(5, 5).length());
    assertEquals(-5, new IntervalUtils.Interval(10, 5).length());
  }
}
