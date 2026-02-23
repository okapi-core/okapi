package org.okapi.rest.traces;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TimestampFilter {
  long tsStartNanos;
  long tsEndNanos;
}
