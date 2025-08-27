package org.okapi.metrics.rollup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class HashFnsTests {

  static final Long now = System.currentTimeMillis();
  static final String PATH_WITH_TENANT = "tenant:series{}";
  static final String PATH_NO_TENANT = "series{}";
  static final String PATH_NO_BRACKETS = "series";

  @ParameterizedTest
  @MethodSource("testInversionTestCases")
  public void testMinutely(String path, Long ts) {
    var key = HashFns.minutelyBucket(path, ts);
    var inverted = HashFns.invertKey(key);
    assertTrue(inverted.isPresent());
    assertEquals(path, inverted.get().timeSeries());
    assertEquals(ts / 60_000, inverted.get().ts());
    assertEquals(HashFns.HASH_TYPE.m, inverted.get().hashType());
  }

  @ParameterizedTest
  @MethodSource("testInversionTestCases")
  public void testHourlyInversion(String path, Long ts) {
    var key = HashFns.hourlyBucket(path, ts);
    var inverted = HashFns.invertKey(key);
    assertTrue(inverted.isPresent());
    assertEquals(path, inverted.get().timeSeries());
    assertEquals(ts / 3600_000, inverted.get().ts());
    assertEquals(HashFns.HASH_TYPE.h, inverted.get().hashType());
  }

  @ParameterizedTest
  @MethodSource("testInversionTestCases")
  public void testSecondlyInversion(String path, Long ts) {
    var key = HashFns.secondlyBucket(path, ts);
    var inverted = HashFns.invertKey(key);
    assertTrue(inverted.isPresent());
    assertEquals(path, inverted.get().timeSeries());
    assertEquals(ts / 1000, inverted.get().ts());
    assertEquals(HashFns.HASH_TYPE.s, inverted.get().hashType());
  }

  public static Stream<Arguments> testInversionTestCases() {
    return Stream.of(
        Arguments.of(PATH_WITH_TENANT, now),
        Arguments.of(PATH_NO_TENANT, now),
        Arguments.of(PATH_NO_BRACKETS, now));
  }
}
