package org.okapi.ds;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.okapi.collections.StringUtils;

public class StringUtilsTests {

  @Test
  public void testHappy() {
    var str = "A:B:C";
    var splits = StringUtils.splitLeft(str, ':', 2);
    assertEquals(Arrays.asList("A", "B", "C"), splits);
  }

  @Test
  public void testLessThanN() {
    var str = "A:B";
    var splits = StringUtils.splitLeft(str, ':', 2);
    assertEquals(Arrays.asList("A", "B"), splits);
  }

  @Test
  public void testNoSplits() {
    var str = "AB";
    var splits = StringUtils.splitLeft(str, ':', 2);
    assertEquals(Arrays.asList("AB"), splits);
  }

  @Test
  public void testMoreThanNAvailable() {
    var str = "A:B:C:D";
    var splits = StringUtils.splitLeft(str, ':', 1);
    assertEquals(Arrays.asList("A", "B:C:D"), splits);
  }

  @Test
  public void testWith0Splits() {
    var str = "A:B:C:D";
    var splits = StringUtils.splitLeft(str, ':', 0);
    assertEquals(Arrays.asList("A:B:C:D"), splits);
  }

  @Test
  public void testWithNegativeSplits() {
    var str = "A:B:C:D";
    var splits = StringUtils.splitLeft(str, ':', -1);
    assertEquals(Arrays.asList("A:B:C:D"), splits);
  }

  @Test
  public void testWithMoreThan2Groups() {
    var str = "A:B:C:D";
    var splits = StringUtils.splitLeft(str, ':', 2);
    assertEquals(Arrays.asList("A", "B", "C:D"), splits);
  }
}
