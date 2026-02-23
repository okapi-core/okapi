package org.okapi.rest.traces;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SpanAttributeHint {
  @NotNull String name;
  @NotNull String type;
}
