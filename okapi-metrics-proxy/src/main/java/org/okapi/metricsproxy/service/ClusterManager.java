package org.okapi.metricsproxy.service;

import static org.okapi.validation.OkapiChecks.checkArgument;
import static org.okapi.validation.OkapiChecks.throwIf;
import static org.okapi.validation.OkapiResponseChecks.*;

import com.google.gson.Gson;
import org.okapi.auth.RoleTemplates;
import org.okapi.metricsproxy.auth.AuthorizationChecker;
import com.okapi.rest.metrics.admin.DiscoveryResponse;
import com.okapi.rest.metrics.admin.NodeMetadataResponse;
import com.okapi.rest.metrics.admin.StartScaleUpRequest;
import com.okapi.rest.metrics.admin.StartScaleUpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.InternalFailureException;
import org.okapi.exceptions.NotFoundException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.metrics.common.ServiceRegistry;

@AllArgsConstructor
@Builder
public class ClusterManager {
  AuthorizationChecker authorizationChecker;
  ServiceRegistry serviceRegistry;
  OkHttpClient okHttpClient;
  String orgId;
  Gson gson;
  String clusterId;

  public DiscoveryResponse listRegisteredNodes(String header) throws Exception {
    var resolved = authorizationChecker.resolve(header);
    var clusterAdminRole = RoleTemplates.getClusterAdminRole(clusterId);
    checkArgument(
        resolved.getAuthorizationRoles().contains(clusterAdminRole), UnAuthorizedException::new);
    return new DiscoveryResponse(listRegisteredMetricNodes());
  }

  public DiscoveryResponse listActiveNodes(String header) throws Exception {
    var resolved = authorizationChecker.resolve(header);
    var clusterAdminRole = RoleTemplates.getClusterAdminRole(clusterId);
    checkArgument(
        resolved.getAuthorizationRoles().contains(clusterAdminRole), UnAuthorizedException::new);
    var maybeActiveNodes = serviceRegistry.listActiveNodes();
    if (maybeActiveNodes.isEmpty()) {
      return new DiscoveryResponse(Collections.emptyList());
    }
    return new DiscoveryResponse(
        maybeActiveNodes.get().stream()
            .map(
                node -> {
                  try {
                    return getMetadata(node);
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                })
            .toList());
  }

  public boolean scaleUpRequired() throws Exception {
    var registered = listRegisteredMetricNodes();
    var activeNodes = serviceRegistry.listActiveNodes();
    if (activeNodes.isEmpty()) {
      return true;
    }

    var setOfActiveNodes = new HashSet<>(activeNodes.get());
    var setOfRegisteredNodes =
        registered.stream().map(NodeMetadataResponse::getId).collect(Collectors.toSet());
    return !setOfActiveNodes.equals(setOfRegisteredNodes);
  }

  public StartScaleUpResponse startScaleUp(String header) throws Exception {
    var resolved = authorizationChecker.resolve(header);
    var clusterAdminRole = RoleTemplates.getClusterAdminRole(clusterId);
    checkArgument(
        resolved.getAuthorizationRoles().contains(clusterAdminRole), UnAuthorizedException::new);
    if (!scaleUpRequired()) {
      var current = serviceRegistry.clusterChangeOp().get();
      return new StartScaleUpResponse(current.opId(), current.state().toString(), current.nodes());
    }
    var registered = listRegisteredMetricNodes();
    var leader = registered.stream().filter(NodeMetadataResponse::isLeader).findFirst();
    checkArgument(leader.isPresent(), InternalFailureException::new);
    var leaderIp = leader.get().getIp();
    var ep = endpoint(leaderIp) + "/api/v1/admin/scale-up";
    StartScaleUpRequest reqBody =
        StartScaleUpRequest.builder()
            .nodes(registered.stream().map(NodeMetadataResponse::getId).toList())
            .build();
    Request req =
        new Request.Builder()
            .post(RequestBody.create(gson.toJson(reqBody).getBytes(StandardCharsets.UTF_8)))
            .header("Content-Type", "application/json")
            .url(ep)
            .build();
    try (var res = okHttpClient.newCall(req).execute()) {
      throwIf(is4xx(res.code()), BadRequestException::new);
      throwIf(is5xx(res.code()), InternalFailureException::new);
      checkArgument(is2xx(res.code()), InternalFailureException::new);
      checkArgument(res.body() != null, InternalFailureException::new);
      return gson.fromJson(res.body().string(), StartScaleUpResponse.class);
    }
  }

  public String endpoint(String ip) {
    return "http://" + ip;
  }

  public List<NodeMetadataResponse> listRegisteredMetricNodes() throws Exception {
    var registered = serviceRegistry.listRegisteredMetricNodes();
    var metadata = new ArrayList<NodeMetadataResponse>();
    for (var node : registered) {
      metadata.add(getMetadata(node));
    }
    return metadata;
  }

  public StartScaleUpResponse getScaleUpOp(String header) throws Exception {
    authorizationChecker.resolve(header);
    var scaleUpOp = serviceRegistry.clusterChangeOp();
    checkArgument(scaleUpOp.isPresent(), NotFoundException::new);
    return new StartScaleUpResponse(
        scaleUpOp.get().opId(), scaleUpOp.get().state().toString(), scaleUpOp.get().nodes());
  }

  public NodeMetadataResponse getMetadata(String id) throws Exception {
    var md = serviceRegistry.getNode(id);
    var path = endpoint(md.ip()) + "/api/v1/admin/meta";
    var req = new Request.Builder().url(path).get().build();
    try (var res = okHttpClient.newCall(req).execute()) {
      throwIf(is4xx(res.code()), BadRequestException::new);
      throwIf(is5xx(res.code()), InternalFailureException::new);
      checkArgument(is2xx(res.code()), InternalFailureException::new);
      checkArgument(res.body() != null, InternalFailureException::new);
      return gson.fromJson(res.body().string(), NodeMetadataResponse.class);
    }
  }
}
