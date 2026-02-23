package org.okapi.metrics.query.rest;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.okapi.metrics.pojos.RES_TYPE;

@AllArgsConstructor
@Builder
@Getter
public class GaugeScanQuery {
  String name;
  Map<String, String> tags;
  long start;
  long end;
  RES_TYPE resType;
}
