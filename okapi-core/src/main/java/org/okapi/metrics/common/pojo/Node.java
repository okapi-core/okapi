package org.okapi.metrics.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Accessors(fluent = true)
public class Node {
  String id;
  String ip;
  NodeState state;
}
