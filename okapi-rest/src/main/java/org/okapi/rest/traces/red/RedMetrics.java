package org.okapi.rest.traces.red;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@Builder
@NoArgsConstructor
public class RedMetrics {
  @NotNull
  List<Long> ts;
  @NotNull
  List<Long> counts;
  @NotNull
  List<Double> durationsP50;
  @NotNull
  List<Double> durationsP75;
  @NotNull
  List<Double> durationsP90;
  @NotNull
  List<Double> durationsP99;
  @NotNull
  List<Long> errors;

  public static RedMetrics of(
      List<Long> ts, List<Long> counts, List<Long> errors, List<Double> values) {
    return RedMetrics.builder()
        .ts(ts)
        .counts(counts)
        .errors(errors)
        .durationsP50(values)
        .durationsP75(values)
        .durationsP90(values)
        .durationsP99(values)
        .build();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || getClass() != other.getClass()) return false;
    RedMetrics that = (RedMetrics) other;
    return Objects.equals(ts, that.ts)
        && Objects.equals(counts, that.counts)
        && Objects.equals(durationsP50, that.durationsP50)
        && Objects.equals(durationsP75, that.durationsP75)
        && Objects.equals(durationsP90, that.durationsP90)
        && Objects.equals(durationsP99, that.durationsP99)
        && Objects.equals(errors, that.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ts, counts, durationsP50, durationsP75, durationsP90, durationsP99, errors);
  }
}
