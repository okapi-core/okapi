/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.agentserver.promql;

import java.util.Map;
import org.okapi.web.service.federation.LegendParser;
import org.okapi.web.tsvector.LegendFn;

public class PromQlLegendParser implements LegendParser<Map<String, String>> {
  @Override
  public LegendFn<Map<String, String>> createLegend(String def) {
    return new PromQlLegendCreator();
  }
}
