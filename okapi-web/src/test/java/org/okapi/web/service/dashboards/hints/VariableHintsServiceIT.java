package org.okapi.web.service.dashboards.hints;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.okapi.web.auth.TestCommons.addToOrg;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Gauge;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.proto.resource.v1.Resource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.web.auth.AbstractIT;
import org.okapi.web.auth.OrgManager;
import org.okapi.web.auth.TokenManager;
import org.okapi.web.auth.UserManager;
import org.okapi.web.dtos.auth.CreateUserRequest;
import org.okapi.web.dtos.auth.SignInRequest;
import org.okapi.web.dtos.constraints.TimeConstraint;
import org.okapi.web.dtos.dashboards.vars.DASH_VAR_TYPE;
import org.okapi.web.dtos.dashboards.vars.GetVarHintsRequest;
import org.okapi.web.service.client.IngesterClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.CONCURRENT)
public class VariableHintsServiceIT extends AbstractIT {

  @Autowired UserManager userManager;
  @Autowired OrgManager orgManager;
  @Autowired TokenManager tokenManager;
  @Autowired RelationGraphDao relationGraphDao;
  @Autowired UsersDao usersDao;
  @Autowired VariableHintsService variableHintsService;
  @Autowired IngesterClient ingesterClient;

  String adminEmail;
  String adminLogin;
  String adminTempToken;
  String orgId;

  @BeforeEach
  public void setupEach() throws Exception {
    super.setup();
    adminEmail = dedup("admin@varhints.com", this.getClass());

    try {
      userManager.signupWithEmailPassword(new CreateUserRequest("Admin", "User", adminEmail, "pw"));
    } catch (Exception ignored) {
    }
    adminLogin = userManager.signInWithEmailPassword(new SignInRequest(adminEmail, "pw"));
    var myOrgs = orgManager.listOrgs(adminLogin);
    orgId = myOrgs.getOrgs().get(0).getOrgId();
    addToOrg(usersDao, relationGraphDao, orgId, adminEmail, true);
    adminTempToken = tokenManager.issueTemporaryToken(adminLogin, orgId);
  }

  @Test
  public void variableHints_ingest_and_query() {
    var now = System.currentTimeMillis();
    var t1 = now - 10_000;
    var t2 = now - 5_000;
    var constraint = TimeConstraint.builder().start(now - 60_000).end(now + 5_000).build();

    var svcA = "svc-varhints-a-" + UUID.randomUUID();
    var svcB = "svc-varhints-b-" + UUID.randomUUID();
    var metricName = "metric.varhints." + UUID.randomUUID();
    var regionA = "us-east-" + UUID.randomUUID();
    var regionB = "us-west-" + UUID.randomUUID();

    var tagsA = Map.of("region", regionA, "env", "dev");
    var tagsB = Map.of("region", regionB, "env", "prod");

    ingesterClient.ingestOtelMetrics(
        buildOtelGauge(
            svcA,
            metricName,
            List.of(numberPointAt(t1, 1.0, tagsA), numberPointAt(t2, 2.0, tagsA))));
    ingesterClient.ingestOtelMetrics(
        buildOtelGauge(
            svcB,
            metricName,
            List.of(numberPointAt(t1, 1.5, tagsB), numberPointAt(t2, 2.5, tagsB))));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var svcHints =
                  variableHintsService.getVarHints(
                      adminTempToken, new GetVarHintsRequest(DASH_VAR_TYPE.SVC, null, constraint));
              log.info("Got hints: {}", svcHints);
              assertNotNull(svcHints);
              assertTrue(svcHints.getSuggestions().containsAll(List.of(svcA, svcB)));

              var metricHints =
                  variableHintsService.getVarHints(
                      adminTempToken,
                      new GetVarHintsRequest(DASH_VAR_TYPE.METRIC, null, constraint));
              assertNotNull(metricHints);
              assertTrue(metricHints.getSuggestions().contains(metricName));

              var tagHints =
                  variableHintsService.getVarHints(
                      adminTempToken,
                      new GetVarHintsRequest(DASH_VAR_TYPE.TAG_VALUE, "region", constraint));
              assertNotNull(tagHints);
              assertTrue(tagHints.getSuggestions().containsAll(List.of(regionA, regionB)));
            });
  }

  private ExportMetricsServiceRequest buildOtelGauge(
      String svc, String metricName, List<NumberDataPoint> points) {
    var gauge = Gauge.newBuilder().addAllDataPoints(points).build();
    return wrapMetric(
        svc, metricName, Metric.newBuilder().setName(metricName).setGauge(gauge).build());
  }

  private ExportMetricsServiceRequest wrapMetric(String svc, String metricName, Metric metric) {
    var scopeMetrics = ScopeMetrics.newBuilder().addMetrics(metric).build();
    var resource =
        Resource.newBuilder()
            .addAttributes(
                KeyValue.newBuilder()
                    .setKey("service.name")
                    .setValue(AnyValue.newBuilder().setStringValue(svc).build())
                    .build())
            .build();
    var resourceMetrics =
        ResourceMetrics.newBuilder().setResource(resource).addScopeMetrics(scopeMetrics).build();
    return ExportMetricsServiceRequest.newBuilder().addResourceMetrics(resourceMetrics).build();
  }

  private NumberDataPoint numberPointAt(long tsMillis, double value, Map<String, String> tags) {
    return NumberDataPoint.newBuilder()
        .setTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(tsMillis))
        .setAsDouble(value)
        .addAllAttributes(toKvList(tags))
        .build();
  }

  private List<KeyValue> toKvList(Map<String, String> tags) {
    return tags.entrySet().stream()
        .map(
            e ->
                KeyValue.newBuilder()
                    .setKey(e.getKey())
                    .setValue(AnyValue.newBuilder().setStringValue(e.getValue()).build())
                    .build())
        .toList();
  }
}
