package org.okapi.rest.metrics.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
public class Histogram {
  long start;
  Long end;
  Long count;
  Float sum;
  List<Integer> counts;
  List<Float> buckets;
}
