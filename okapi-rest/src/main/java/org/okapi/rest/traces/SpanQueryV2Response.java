package org.okapi.rest.traces;

import java.util.List;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SpanQueryV2Response {
  List<SpanRowV2> items;
}
