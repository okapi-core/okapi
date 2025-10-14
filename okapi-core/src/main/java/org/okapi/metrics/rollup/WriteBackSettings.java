package org.okapi.metrics.rollup;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.clock.Clock;

@AllArgsConstructor
@Getter
public class WriteBackSettings {
  Duration hotWindow;
  Clock clock;
}
