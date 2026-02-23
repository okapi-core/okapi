package org.okapi.agent.dto;

import lombok.Builder;

@Builder
public record QuerySpec(String serializedQuery) {}
