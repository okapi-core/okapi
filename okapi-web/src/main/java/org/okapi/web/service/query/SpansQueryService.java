/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.query;

import lombok.RequiredArgsConstructor;
import org.okapi.rest.traces.SpanAttributeHintsRequest;
import org.okapi.rest.traces.SpanAttributeHintsResponse;
import org.okapi.rest.traces.SpanAttributeValueHintsRequest;
import org.okapi.rest.traces.SpanAttributeValueHintsResponse;
import org.okapi.rest.traces.SpanQueryV2Request;
import org.okapi.rest.traces.SpanQueryV2Response;
import org.okapi.rest.traces.SpansFlameGraphResponse;
import org.okapi.rest.traces.SpansQueryStatsRequest;
import org.okapi.rest.traces.SpansQueryStatsResponse;
import org.okapi.web.service.access.OrgMemberChecker;
import org.okapi.web.service.client.IngesterClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpansQueryService {

  private final IngesterClient ingesterClient;
  private final OrgMemberChecker orgMemberChecker;

  public SpanQueryV2Response querySpans(String token, SpanQueryV2Request request) {
    orgMemberChecker.checkUserIsOrgMember(token);
    return ingesterClient.querySpans(request);
  }

  public SpansFlameGraphResponse queryFlameGraph(String token, SpanQueryV2Request request) {
    orgMemberChecker.checkUserIsOrgMember(token);
    return ingesterClient.querySpansFlameGraph(request);
  }

  public SpansQueryStatsResponse getSpansStats(String token, SpansQueryStatsRequest request) {
    orgMemberChecker.checkUserIsOrgMember(token);
    return ingesterClient.getSpansStats(request);
  }

  public SpanAttributeHintsResponse getAttributeHints(
      String token, SpanAttributeHintsRequest request) {
    orgMemberChecker.checkUserIsOrgMember(token);
    return ingesterClient.getSpanAttributeHints(request);
  }

  public SpanAttributeValueHintsResponse getAttributeValueHints(
      String token, SpanAttributeValueHintsRequest request) {
    orgMemberChecker.checkUserIsOrgMember(token);
    return ingesterClient.getSpanAttributeValueHints(request);
  }
}
