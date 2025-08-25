package org.okapi.metrics.rollup;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.clock.Clock;

import java.time.Duration;

@AllArgsConstructor
@Getter
public class WriteBackSettings {
    Duration hotWindow;
    Clock clock;
}
