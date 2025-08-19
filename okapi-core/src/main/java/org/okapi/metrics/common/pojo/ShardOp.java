package org.okapi.metrics.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Accessors(fluent = true)
public class ShardOp {
  String opId;
  int nShards;
  TWO_PHASE_STATE state;
  Long updateTime;
  Long startTime;
}
