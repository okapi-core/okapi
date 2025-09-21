package org.okapi.metrics.pojos.results;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;

@AllArgsConstructor
@Value
public class HistoScan {
  String universalPath;
  long start;
  long end;
  List<Float> ubs;
  List<Integer> counts;
}
