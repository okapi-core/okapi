/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.federation.hydration;

import java.util.Map;
import org.okapi.exceptions.BadRequestException;
import org.okapi.web.service.QueryHydrator;
import org.springframework.stereotype.Service;

@Service
public class QueryHydratorImpl implements QueryHydrator {
  @Override
  public String hydrate(String query, Map<String, Object> params) throws BadRequestException {
    String hydratedQuery = query;
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      String placeholder = "{{" + entry.getKey() + "}}";
      hydratedQuery = hydratedQuery.replace(placeholder, entry.getValue().toString());
    }
    if (hydratedQuery.contains("{{") || hydratedQuery.contains("}}")) {
      throw new BadRequestException(
          "Not all placeholders were replaced in the query: " + hydratedQuery);
    }
    return hydratedQuery;
  }
}
