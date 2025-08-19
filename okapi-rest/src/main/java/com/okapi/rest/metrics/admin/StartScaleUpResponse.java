package com.okapi.rest.metrics.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
@Builder
public class StartScaleUpResponse {
    String opId;
    String state;
    List<String> nodeIds;
}
