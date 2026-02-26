package org.okapi.rest.traces.red;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

public class RedMetricsTest {

  @Test
  void equalsMatchesAllFields() {
    var a =
        RedMetrics.builder()
            .ts(List.of(1L, 2L))
            .counts(List.of(3L, 4L))
            .errors(List.of(0L, 1L))
            .durationsP50(List.of(1.0, 2.0))
            .durationsP75(List.of(1.1, 2.1))
            .durationsP90(List.of(1.2, 2.2))
            .durationsP99(List.of(1.3, 2.3))
            .build();
    var b =
        RedMetrics.builder()
            .ts(List.of(1L, 2L))
            .counts(List.of(3L, 4L))
            .errors(List.of(0L, 1L))
            .durationsP50(List.of(1.0, 2.0))
            .durationsP75(List.of(1.1, 2.1))
            .durationsP90(List.of(1.2, 2.2))
            .durationsP99(List.of(1.3, 2.3))
            .build();
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void equalsDetectsDifferences() {
    var base =
        RedMetrics.builder()
            .ts(List.of(1L))
            .counts(List.of(1L))
            .errors(List.of(0L))
            .durationsP50(List.of(1.0))
            .durationsP75(List.of(1.0))
            .durationsP90(List.of(1.0))
            .durationsP99(List.of(1.0))
            .build();
    var different =
        RedMetrics.builder()
            .ts(List.of(1L))
            .counts(List.of(2L))
            .errors(List.of(0L))
            .durationsP50(List.of(1.0))
            .durationsP75(List.of(1.0))
            .durationsP90(List.of(1.0))
            .durationsP99(List.of(1.0))
            .build();
    assertNotEquals(base, different);
  }
}
