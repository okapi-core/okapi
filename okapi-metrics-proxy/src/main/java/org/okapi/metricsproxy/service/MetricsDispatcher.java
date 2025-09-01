package org.okapi.metricsproxy.service;

import static org.okapi.http.ResponseDecoders.translateResponse;
import static org.okapi.validation.OkapiChecks.checkArgument;

import com.google.gson.Gson;
import org.okapi.auth.RoleTemplates;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.metrics.IdCreator;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metricsproxy.auth.AuthorizationChecker;
import org.okapi.rest.metrics.SubmitMetricsRequest;
import org.okapi.rest.metrics.SubmitMetricsRequestInternal;
import org.okapi.rest.metrics.SubmitMetricsResponse;
import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

@AllArgsConstructor
public class MetricsDispatcher {

  ZkRegistry listenerBasedRegistry;
  OkHttpClient okHttpClient;
  AuthorizationChecker authorizationChecker;
  Gson gson;

  public MetricsDispatcher(
      ZkRegistry listenerBasedRegistry,
      OkHttpClient httpClient,
      AuthorizationChecker authorizationChecker) {
    this.listenerBasedRegistry = listenerBasedRegistry;
    this.okHttpClient = httpClient;
    this.gson = new Gson();
    this.authorizationChecker = authorizationChecker;
  }

  public SubmitMetricsResponse forward(String authHeader, SubmitMetricsRequest requestV2)
      throws Exception {
    var tokenDto = authorizationChecker.resolve(authHeader);
    var orgId = tokenDto.getOrgId();
    var teamId = tokenDto.getTeamId();
    checkArgument(orgId != null, UnAuthorizedException::new);
    checkArgument(teamId != null, UnAuthorizedException::new);
    var writerRole = RoleTemplates.getTeamWriterRole(orgId, teamId);
    checkArgument(
        tokenDto.getAuthorizationRoles().contains(writerRole), UnAuthorizedException::new);
    var tenantId = IdCreator.getTenantId(orgId, teamId);
    var routable =
        SubmitMetricsRequestInternal.builder()
            .ts(requestV2.getTs())
            .values(requestV2.getValues())
            .tags(requestV2.getTags())
            .metricName(requestV2.getMetricName())
            .tenantId(tenantId)
            .build();
    var path = MetricPaths.convertToPath(routable);
    var node = listenerBasedRegistry.route(path);
    var body = gson.toJson(routable).getBytes(StandardCharsets.UTF_8);
    var request =
        new Request.Builder()
            .url(getUrl(node))
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(body))
            .build();
    try (var postResponse = okHttpClient.newCall(request).execute()) {
      translateResponse(postResponse);
      return new SubmitMetricsResponse("OK");
    }
  }

  public String getUrl(Node node) {
    return "http://" + node.ip() + "/api/v1/metrics";
  }
}
