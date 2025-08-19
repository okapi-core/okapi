package com.okapi.rest.metrics;

import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Builder
@Getter
public class GetMetricsResponse {
  String tenant;
  String name;
  Map<String, String> tags;
  RES_TYPE resolution;
  AGG_TYPE aggregation;
  List<Long> times;
  List<Float> values;
}
