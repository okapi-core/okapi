package org.okapi.rest.traces;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TimestampMillisFilter {
  long tsMillisStart;
  long tsMillisEnd;
}
