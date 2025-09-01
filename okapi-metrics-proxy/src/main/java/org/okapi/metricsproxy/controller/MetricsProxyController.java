package org.okapi.metricsproxy.controller;

import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.NotFoundException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.headers.CookiesAndHeaders;
import org.okapi.metricsproxy.service.ClusterManager;
import org.okapi.metricsproxy.service.MetricsDispatcher;
import org.okapi.metricsproxy.service.ScanQueryProcessor;
import org.okapi.rest.metrics.*;
import org.okapi.rest.metrics.admin.DiscoveryResponse;
import org.okapi.rest.metrics.admin.StartScaleUpResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsProxyController {

  @Autowired MetricsDispatcher metricsDispatcher;
  @Autowired ScanQueryProcessor scanQueryProcessor;
  @Autowired ClusterManager clusterManager;

  @PostMapping("")
  public SubmitMetricsResponse submit(
      @RequestHeader("Authorization") String authHeader,
      @Valid @RequestBody SubmitMetricsRequest request)
      throws Exception {
    return metricsDispatcher.forward(authHeader, request);
  }

  @PostMapping("/query")
  public GetMetricsResponse query(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @RequestBody @Validated GetMetricsRequest getMetricsRequest)
      throws Exception {
    return scanQueryProcessor.getMetricsResponse(tempToken, getMetricsRequest);
  }

  @PostMapping("/s")
  public SearchMetricsResponse search(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @RequestBody SearchMetricsRequest searchMetricsRequest)
      throws UnAuthorizedException, BadRequestException, NotFoundException {
    return scanQueryProcessor.searchMetrics(tempToken, searchMetricsRequest);
  }

  @GetMapping("/registered-nodes")
  public DiscoveryResponse listRegistered(@RequestHeader("Authorization") String authHeader)
      throws Exception {
    return clusterManager.listRegisteredNodes(authHeader);
  }

  @GetMapping("/active-nodes")
  public DiscoveryResponse listActive(@RequestHeader("Authorization") String authHeader)
      throws Exception {
    return clusterManager.listActiveNodes(authHeader);
  }

  @PostMapping("/scale-up")
  public StartScaleUpResponse scaleup(@RequestHeader("Authorization") String authHeader)
      throws Exception {
    return clusterManager.startScaleUp(authHeader);
  }

  @GetMapping("/scale-up-op")
  public StartScaleUpResponse getScaleUpOp(@RequestHeader("Authorization") String authHeader)
      throws Exception {
    return clusterManager.getScaleUpOp(authHeader);
  }
}
