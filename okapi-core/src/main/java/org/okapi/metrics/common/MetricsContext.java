package org.okapi.metrics.common;

import lombok.AllArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
public class MetricsContext {
    String contextId;

    public Optional<String> getContextId() {
        return Optional.ofNullable(contextId);
    }

    public static MetricsContext createContext(String id){
        return new MetricsContext(id);
    }
}
