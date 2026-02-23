package org.okapi.metrics.query;

public interface MetadataFilter<M> {
    boolean shouldRead(M metadata);
}
