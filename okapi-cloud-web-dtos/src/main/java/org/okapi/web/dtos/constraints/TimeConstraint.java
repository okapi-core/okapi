package org.okapi.web.dtos.constraints;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class TimeConstraint {
    long start;
    long end;
}
