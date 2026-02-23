package org.okapi.rest.traces;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SpanAttributeHintsResponse {
  @NotNull List<SpanAttributeHint> defaultAttributes;
  @NotNull List<SpanAttributeHint> customAttributes;
}
