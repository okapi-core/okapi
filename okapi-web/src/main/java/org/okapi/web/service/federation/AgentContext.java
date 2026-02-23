/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.federation;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.okapi.web.dtos.federation.FederatedQueryRequest;

@AllArgsConstructor
@Getter
@Builder
public class AgentContext {
  String sourceId;
  String query;
  Map<String, Object> queryParams;
  String legendFn;

  public static AgentContext makeContext(FederatedQueryRequest request) {
    return AgentContext.builder()
        .query(request.getQuery())
        .queryParams(request.getQueryContext())
        .legendFn(request.getLegendFn())
        .build();
  }
}
