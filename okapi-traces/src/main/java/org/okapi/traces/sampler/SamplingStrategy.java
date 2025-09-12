package org.okapi.traces.sampler;

public interface SamplingStrategy {
    // Given a trace id, return true if the trace should be sampled.
    boolean sample(String traceId);
}
