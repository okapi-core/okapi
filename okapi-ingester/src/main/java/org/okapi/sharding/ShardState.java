package org.okapi.sharding;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ShardState {
    STEADY, MOVING
}
