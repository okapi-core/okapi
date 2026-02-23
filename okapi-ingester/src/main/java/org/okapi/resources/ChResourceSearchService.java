/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.resources;

import com.clickhouse.client.api.Client;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.okapi.ch.ChJteTemplateFiles;
import org.okapi.metrics.ch.ChConstants;
import org.okapi.metrics.ch.template.ChMetricTemplateEngine;
import org.okapi.rest.metrics.query.METRIC_TYPE;
import org.okapi.rest.search.SearchResourcesRequest;
import org.okapi.rest.search.SearchResourcesResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChResourceSearchService {
  private final Client client;
  private final ChMetricTemplateEngine templateEngine;

  public SearchResourcesResponse search(SearchResourcesRequest request) {
    var filter = request.getMetricEventFilter();
    if (filter == null) {
      var resources = new LinkedHashSet<String>();
      for (var metricType : METRIC_TYPE.values()) {
        resources.addAll(runQuery(metricType, request.getStart(), request.getEnd()));
      }
      return new SearchResourcesResponse(new ArrayList<>(resources));
    }

    var metricType = filter.getMetricType();
    return new SearchResourcesResponse(runQuery(metricType, request.getStart(), request.getEnd()));
  }

  private List<String> runQuery(METRIC_TYPE metricType, long startMs, long endMs) {
    var query = renderQuery(metricType, startMs, endMs);
    var records = client.queryAll(query);
    var resources = new ArrayList<String>(records.size());
    for (var record : records) {
      resources.add(record.getString("resource"));
    }
    return resources;
  }

  private String renderQuery(METRIC_TYPE metricType, long startMs, long endMs) {
    var templateData =
        ResourceSearchQueryTemplate.builder()
            .table(tableFor(metricType))
            .startMs(startMs)
            .endMs(endMs)
            .build();

    var templateName =
        switch (metricType) {
          case GAUGE -> ChJteTemplateFiles.SEARCH_RESOURCES_GAUGE;
          case HISTO -> ChJteTemplateFiles.SEARCH_RESOURCES_HISTO;
          case SUM -> ChJteTemplateFiles.SEARCH_RESOURCES_SUM;
        };

    TemplateOutput output = new StringOutput();
    templateEngine.render(templateName, templateData, output);
    return output.toString();
  }

  private static String tableFor(METRIC_TYPE metricType) {
    return switch (metricType) {
      case GAUGE -> ChConstants.TBL_GAUGES;
      case HISTO -> ChConstants.TBL_HISTOS;
      case SUM -> ChConstants.TBL_SUM;
    };
  }
}
