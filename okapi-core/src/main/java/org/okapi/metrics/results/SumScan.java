package org.okapi.metrics.results;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
public class SumScan {
  String universalPath;
  List<Long> ts;
  long windowSize;
  List<Integer> counts;
}
