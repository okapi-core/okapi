/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools.backends.datadog;

import static org.okapi.web.ai.tools.HttpQuerySerializer.serialize;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import org.okapi.agent.dto.AgentQueryRecords;
import org.okapi.agent.dto.HTTP_METHOD;
import org.okapi.agent.dto.QuerySpec;
import org.okapi.web.ai.tools.params.LogQuery;
import org.okapi.web.ai.tools.params.SpanQuery;
import org.okapi.web.ai.tools.params.TimeSeriesQuery;

public class DatadogQueryWriter {

  public QuerySpec writeGetTsQuery(TimeSeriesQuery query) {
    return buildMetricsQuery("/api/v1/query", query);
  }

  public QuerySpec writeGetDistributionQuery(TimeSeriesQuery query) {
    // Datadog distributions use the same query endpoint; the downstream query string determines
    // whether a distribution/ histogram is returned.
    return buildMetricsQuery("/api/v1/query", query);
  }

  public QuerySpec writeGetSpanQuery(SpanQuery query) {
    long fromMillis = query.getStartTime();
    long toMillis = query.getEndTime();
    String expr = buildSpanQueryExpression(query);
    String encodedExpr = URLEncoder.encode(expr, StandardCharsets.UTF_8);
    String path =
        "/api/v2/spans/events"
            + "?filter[query]="
            + encodedExpr
            + "&filter[from]="
            + fromMillis
            + "&filter[to]="
            + toMillis;
    AgentQueryRecords.HttpQuery httpQuery =
        new AgentQueryRecords.HttpQuery(HTTP_METHOD.GET, path, Map.of(), null);
    return serialize(httpQuery);
  }

  public QuerySpec writeGetLogQuery(LogQuery query) {
    throw new UnsupportedOperationException("Log queries are not implemented for Datadog yet.");
  }

  private QuerySpec buildMetricsQuery(String basePath, TimeSeriesQuery query) {
    long fromSeconds = query.getStartTime() / 1000;
    long toSeconds = query.getEndTime() / 1000;
    String expr = buildQueryExpression(query);
    String encodedExpr = URLEncoder.encode(expr, StandardCharsets.UTF_8);

    StringBuilder path =
        new StringBuilder(basePath)
            .append("?from=")
            .append(fromSeconds)
            .append("&to=")
            .append(toSeconds)
            .append("&query=")
            .append(encodedExpr);
    if (query.getResolution() != null) {
      path.append("&interval=").append(query.getResolution() / 1000);
    }
    AgentQueryRecords.HttpQuery httpQuery =
        new AgentQueryRecords.HttpQuery(HTTP_METHOD.GET, path.toString(), Map.of(), null);
    return serialize(httpQuery);
  }

  private String buildQueryExpression(TimeSeriesQuery query) {
    StringBuilder builder = new StringBuilder(query.getPathName());
    if (query.getTags() != null && !query.getTags().isEmpty()) {
      String tagString =
          query.getTags().entrySet().stream()
              .map(entry -> entry.getKey() + ":" + entry.getValue())
              .collect(Collectors.joining(","));
      builder.append("{").append(tagString).append("}");
    }
    return builder.toString();
  }

  private String buildSpanQueryExpression(SpanQuery query) {
    if (query.getLabelFilter() == null || query.getLabelFilter().getLabels() == null) {
      return "*";
    }
    return query.getLabelFilter().getLabels().entrySet().stream()
        .map(e -> e.getKey() + ":" + e.getValue())
        .collect(Collectors.joining(" AND "));
  }
}
