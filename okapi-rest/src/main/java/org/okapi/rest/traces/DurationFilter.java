package org.okapi.rest.traces;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DurationFilter {
  @NotNull long durMinMillis;
  @NotNull long durMaxMillis;
}
