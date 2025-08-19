package org.okapi.metrics.common.pojo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Accessors(fluent = true)
public class ClusterConfig {
    String opId;
    List<String> nodes;
}
