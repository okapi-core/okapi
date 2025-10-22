package org.okapi.swim.membership;

import java.util.Set;
import org.okapi.swim.ping.Member;

public interface TimelineBuilder {
  /** Build the alive member set for the given hour (hour start in epoch millis). */
  Set<Member> buildAliveSetForHour(long hourStartMillis);
}

