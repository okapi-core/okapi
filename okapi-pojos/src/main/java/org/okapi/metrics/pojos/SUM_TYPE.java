package org.okapi.metrics.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum SUM_TYPE {
  DELTA("delta"),
  CSUM("csum");
  @Getter private String sumType;
}
