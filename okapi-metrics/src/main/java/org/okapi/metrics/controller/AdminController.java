package org.okapi.metrics.controller;

import static org.okapi.validation.OkapiChecks.checkArgument;

import org.okapi.rest.metrics.admin.NodeMetadataResponse;
import org.okapi.rest.metrics.admin.StartScaleUpRequest;
import org.okapi.rest.metrics.admin.StartScaleUpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.ZkResources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
  @Autowired ServiceRegistry serviceRegistry;

  @Autowired(required = false)
  ZkResources zkResources;

  @GetMapping("/meta")
  public NodeMetadataResponse nodeMetadataResponse() throws Exception {
    var self = serviceRegistry.getSelf();
    var isLeader = false;
    if (zkResources != null) {
      isLeader = zkResources.isLeader();
    }
    return new NodeMetadataResponse(self.ip(), self.id(), isLeader);
  }

  @PostMapping("/scale-up")
  public StartScaleUpResponse scaleUp(@RequestBody StartScaleUpRequest startScaleUpRequest)
      throws Exception {
    var registered = new HashSet<>(registered());
    var required = new HashSet<>(startScaleUpRequest.getNodes());
    if (registered.equals(required)) {
      return getOp();
    }
    serviceRegistry.safelyUpdateNodes(startScaleUpRequest.getNodes());
    var clusterOp = serviceRegistry.clusterChangeOp().get();
    return StartScaleUpResponse.builder()
        .nodeIds(startScaleUpRequest.getNodes())
        .opId(clusterOp.opId())
        .build();
  }

  @GetMapping("/registered")
  public List<String> registered() throws Exception {
    return new ArrayList<>(serviceRegistry.listRegisteredMetricNodes());
  }

  @GetMapping("/scale-up")
  public StartScaleUpResponse getOp() throws Exception {
    var clusterOp = serviceRegistry.clusterChangeOp();
    checkArgument(clusterOp.isPresent(), BadRequestException::new);
    return StartScaleUpResponse.builder()
        .state(clusterOp.get().state().toString())
        .nodeIds(clusterOp.get().nodes())
        .opId(clusterOp.get().opId())
        .build();
  }
}
