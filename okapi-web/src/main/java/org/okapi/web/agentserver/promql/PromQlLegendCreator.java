/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.agentserver.promql;

import java.util.Map;
import java.util.TreeMap;
import org.okapi.web.tsvector.LegendFn;

public class PromQlLegendCreator implements LegendFn<Map<String, String>> {
  @Override
  public String getLegend(Map<String, String> base) {
    var sortedMap = new TreeMap<>(base);
    var name = base.get("__name__");
    var legend = new StringBuilder();
    legend.append(name);
    // convert to a prometheus path
    var nameValuePairs =
        sortedMap.entrySet().stream()
            .filter(e -> !e.getKey().equals("__name__"))
            .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
            .toList();
    if (!nameValuePairs.isEmpty()) {
      legend.append("{");
      legend.append(String.join(",", nameValuePairs));
      legend.append("}");
    }
    return legend.toString();
  }
}
