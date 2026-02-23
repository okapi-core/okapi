package org.okapi.rest.metrics.query;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
public class GetGaugeResponse {
  RES_TYPE resolution;
  AGG_TYPE aggregation;
  List<Long> times;
  List<Float> values;
}
