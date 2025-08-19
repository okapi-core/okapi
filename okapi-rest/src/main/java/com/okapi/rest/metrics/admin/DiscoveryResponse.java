package com.okapi.rest.metrics.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
public class DiscoveryResponse {
    List<NodeMetadataResponse> registered;
}
