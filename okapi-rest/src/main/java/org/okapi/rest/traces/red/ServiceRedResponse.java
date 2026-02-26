package org.okapi.rest.traces.red;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ServiceRedResponse {
  // RED for that specific service
  // RED for each operation
  // edge-RED for each of this service.
  // op-RED for operation level REDs
  String service;
  RedMetrics serviceRed;
  List<ServiceEdgeRed> peerReds;
  List<ServiceOpRed> serviceOpReds;
  Integer totalDetectedOps;
}
