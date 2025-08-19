package org.okapi.metrics;

import java.util.Optional;

public interface NodeStateRegistry {
    void updateLastCheckPointedHour(long hr) throws Exception;
    Optional<Long> getLastCheckpointedHour() throws Exception;
}
