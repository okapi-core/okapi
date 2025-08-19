package org.okapi.metrics.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Accessors(fluent = true)
public class ClusterChangeOp {
  String opId;
  List<String> nodes;
  TWO_PHASE_STATE state;
  Long updateTime;
  Long startTime;
}
