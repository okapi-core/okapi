package org.okapi.metrics.ch.rows;

import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class ChSearchMetricsRow {
  String name;
  Map<String, String> tags;

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ChSearchMetricsRow that = (ChSearchMetricsRow) o;
    return Objects.equals(name, that.name) && Objects.equals(tags, that.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, tags);
  }
}
