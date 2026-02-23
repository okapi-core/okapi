/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.controller;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.okapi.web.auth.TestCommons.addToOrg;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.dao.UsersDao;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.rest.traces.HttpFilters;
import org.okapi.rest.traces.NumericalAggConfig;
import org.okapi.rest.traces.ServiceFilter;
import org.okapi.rest.traces.SpanAttributeHintsRequest;
import org.okapi.rest.traces.SpanAttributeHintsResponse;
import org.okapi.rest.traces.SpanAttributeValueHintsRequest;
import org.okapi.rest.traces.SpanAttributeValueHintsResponse;
import org.okapi.rest.traces.SpanQueryV2Request;
import org.okapi.rest.traces.SpanQueryV2Response;
import org.okapi.rest.traces.SpansQueryStatsRequest;
import org.okapi.rest.traces.SpansQueryStatsResponse;
import org.okapi.rest.traces.TimestampFilter;
import org.okapi.rest.traces.TimestampMillisFilter;
import org.okapi.web.auth.AbstractIT;
import org.okapi.web.auth.OrgManager;
import org.okapi.web.auth.TokenManager;
import org.okapi.web.auth.UserManager;
import org.okapi.web.dtos.auth.CreateUserRequest;
import org.okapi.web.dtos.auth.SignInRequest;
import org.okapi.web.service.client.IngesterClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.CONCURRENT)
public class SpansQueryIT extends AbstractIT {

  @Autowired UserManager userManager;
  @Autowired OrgManager orgManager;
  @Autowired TokenManager tokenManager;
  @Autowired RelationGraphDao relationGraphDao;
  @Autowired UsersDao usersDao;
  @Autowired IngesterClient ingesterClient;
  @Autowired SpansQueryController spansQueryController;

  String adminEmail;
  String adminLogin;
  String adminTempToken;
  String orgId;

  @BeforeEach
  public void setupEach() throws Exception {
    super.setup();
    adminEmail = dedup("admin@spanqueries.com", this.getClass());

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
  public void spans_query_hints_and_stats() {
    var now = System.currentTimeMillis();
    var t1 = now - 10_000;
    var t2 = now - 5_000;

    var svc = "svc-span-" + UUID.randomUUID();
    var traceId = "trace-span-" + UUID.randomUUID();
    var spanId = "span-span-" + UUID.randomUUID();

    ingesterClient.ingestOtelTraces(buildOtelSpan(svc, traceId, spanId, t1, t2));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var query =
                  SpanQueryV2Request.builder()
                      .serviceFilter(ServiceFilter.builder().service(svc).build())
                      .httpFilters(HttpFilters.builder().httpMethod("GET").build())
                      .timestampFilter(
                          TimestampFilter.builder()
                              .tsStartNanos(TimeUnit.MILLISECONDS.toNanos(now - 60_000))
                              .tsEndNanos(TimeUnit.MILLISECONDS.toNanos(now + 5_000))
                              .build())
                      .build();
              SpanQueryV2Response spans = spansQueryController.querySpans(adminTempToken, query);
              assertNotNull(spans);
              assertTrue(spans.getItems().size() >= 1);

              var hintsReq =
                  SpanAttributeHintsRequest.builder()
                      .timestampFilter(
                          TimestampMillisFilter.builder()
                              .tsMillisStart(now - 60_000)
                              .tsMillisEnd(now + 5_000)
                              .build())
                      .build();
              SpanAttributeHintsResponse hints =
                  spansQueryController.getAttributeHints(adminTempToken, hintsReq);
              assertNotNull(hints);
              assertTrue(hints.getDefaultAttributes().size() > 0);
              assertTrue(
                  hints.getCustomAttributes().stream()
                      .anyMatch(h -> "custom.string".equals(h.getName())));

              var valueReq =
                  SpanAttributeValueHintsRequest.builder()
                      .attributeName("custom.string")
                      .timestampFilter(
                          TimestampMillisFilter.builder()
                              .tsMillisStart(now - 60_000)
                              .tsMillisEnd(now + 5_000)
                              .build())
                      .build();
              SpanAttributeValueHintsResponse valueHints =
                  spansQueryController.getAttributeValueHints(adminTempToken, valueReq);
              assertNotNull(valueHints);
              assertTrue(valueHints.getValues().contains("alpha"));

              var statsReq =
                  SpansQueryStatsRequest.builder()
                      .attributes(List.of("custom.number"))
                      .numericalAgg(
                          NumericalAggConfig.builder()
                              .aggregation(AGG_TYPE.AVG)
                              .resType(RES_TYPE.HOURLY)
                              .build())
                      .timestampFilter(
                          TimestampFilter.builder()
                              .tsStartNanos(TimeUnit.MILLISECONDS.toNanos(now - 60_000))
                              .tsEndNanos(TimeUnit.MILLISECONDS.toNanos(now + 5_000))
                              .build())
                      .build();
              SpansQueryStatsResponse stats =
                  spansQueryController.getSpansStats(adminTempToken, statsReq);
              assertNotNull(stats);
              assertTrue(stats.getCount() >= 1);
              assertTrue(stats.getNumericSeries().size() >= 1);
            });
  }

  private ExportTraceServiceRequest buildOtelSpan(
      String serviceName, String traceId, String spanId, long startMillis, long endMillis) {
    var resource = Resource.newBuilder().addAttributes(attr("service.name", serviceName)).build();
    var span =
        Span.newBuilder()
            .setTraceId(utf8Bytes(traceId))
            .setSpanId(utf8Bytes(spanId))
            .setName("span-it")
            .setKind(Span.SpanKind.SPAN_KIND_SERVER)
            .setStartTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(startMillis))
            .setEndTimeUnixNano(TimeUnit.MILLISECONDS.toNanos(endMillis))
            .addAttributes(attr("http.request.method", "GET"))
            .addAttributes(attr("http.response.status_code", 200))
            .addAttributes(attr("custom.string", "alpha"))
            .addAttributes(attr("custom.number", 42))
            .build();
    var scope = ScopeSpans.newBuilder().addSpans(span).build();
    return ExportTraceServiceRequest.newBuilder()
        .addResourceSpans(ResourceSpans.newBuilder().setResource(resource).addScopeSpans(scope))
        .build();
  }

  private KeyValue attr(String key, String value) {
    return KeyValue.newBuilder()
        .setKey(key)
        .setValue(AnyValue.newBuilder().setStringValue(value))
        .build();
  }

  private KeyValue attr(String key, int value) {
    return KeyValue.newBuilder()
        .setKey(key)
        .setValue(AnyValue.newBuilder().setIntValue(value))
        .build();
  }

  private com.google.protobuf.ByteString utf8Bytes(String value) {
    return com.google.protobuf.ByteString.copyFromUtf8(value);
  }
}
