/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.agent.parser;

import java.util.function.Function;
import org.okapi.agent.dto.AgentQueryRecords;
import org.okapi.agent.dto.QueryProcessingError;

public class QueryParser {
  public Function<String, AgentQueryRecords.HttpQuery> httpQueryParser;

  public Object parse(String query) {
    var type = query.split(" ", 2);
    var queryType = type.length > 0 ? type[0] : "";
    var queryContent = type.length > 1 ? type[1] : "";
    switch (queryType) {
      case "HTTP" -> {
        return httpQueryParser.apply(queryContent);
      }
      default -> {
        return new QueryProcessingError("Unsupported query type: " + queryType);
      }
    }
  }
}
