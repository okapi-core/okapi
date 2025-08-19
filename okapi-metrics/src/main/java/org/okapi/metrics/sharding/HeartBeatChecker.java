package org.okapi.metrics.sharding;

import org.okapi.clock.Clock;
import lombok.AllArgsConstructor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@AllArgsConstructor
public class HeartBeatChecker {
    Clock clock;
    public static final Duration HEART_BEAT_DURATION = Duration.of(5, ChronoUnit.MINUTES);

    public boolean isHealthy(long lastBeat){
        return clock.currentTimeMillis() - lastBeat <= HEART_BEAT_DURATION.toMillis();
    }
}
