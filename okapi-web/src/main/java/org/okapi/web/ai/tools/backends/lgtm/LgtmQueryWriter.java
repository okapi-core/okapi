/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools.backends.lgtm;

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

public class LgtmQueryWriter {

  public QuerySpec writeGetTsQuery(TimeSeriesQuery query) {

    var path =
        buildPath(
            "/api/v1/query_range", query.getPathName(), query.getStartTime(), query.getEndTime());
    var httpQuery = new AgentQueryRecords.HttpQuery(HTTP_METHOD.GET, path, Map.of(), null);
    return serialize(httpQuery);
  }

  public QuerySpec writeGetSpanQuery(SpanQuery query) {
    throw new UnsupportedOperationException("Span queries are not implemented for LGTM yet.");
  }

  public QuerySpec writeGetLogQuery(LogQuery query) {
    throw new UnsupportedOperationException("Log queries are not implemented for LGTM yet.");
  }

  private void appendTags(StringBuilder builder, Map<String, String> tags) {
    if (tags == null || tags.isEmpty()) {
      return;
    }
    var tagString =
        tags.entrySet().stream()
            .map(e -> e.getKey() + ":" + e.getValue())
            .collect(Collectors.joining(","));
    builder.append("{").append(tagString).append("}");
  }

  private String buildPath(String basePath, String queryExpr, long startMillis, long endMillis) {
    var fromSeconds = startMillis / 1000;
    var toSeconds = endMillis / 1000;
    var encodedQuery = URLEncoder.encode(queryExpr, StandardCharsets.UTF_8);
    return basePath + "?query=" + encodedQuery + "&from=" + fromSeconds + "&to=" + toSeconds;
  }
}
