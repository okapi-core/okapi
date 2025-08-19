package org.okapi.metrics.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Getter
@Accessors(fluent = true)
@Builder(toBuilder = true)
public class ShardConfig {
    String opId;
    int nShards;
}
