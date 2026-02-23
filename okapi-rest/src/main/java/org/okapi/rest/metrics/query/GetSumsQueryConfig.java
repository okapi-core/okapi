package org.okapi.rest.metrics.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class GetSumsQueryConfig {
  public enum TEMPORALITY {
    DELTA_AGGREGATE,
    CUMULATIVE
  }

  TEMPORALITY temporality;
}
