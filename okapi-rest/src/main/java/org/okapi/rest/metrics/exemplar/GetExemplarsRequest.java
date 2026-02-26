package org.okapi.rest.metrics.exemplar;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.okapi.rest.traces.TimestampFilter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class GetExemplarsRequest {
  String svc;
  String metric;
  Map<String, String> labels;
  TimestampFilter timeFilter;
}
