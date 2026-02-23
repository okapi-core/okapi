package org.okapi.rest.traces;

import java.util.List;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SpanQueryResponse {
  private List<SpanDto> items;

  public List<SpanDto> items() {
    return items;
  }
}
