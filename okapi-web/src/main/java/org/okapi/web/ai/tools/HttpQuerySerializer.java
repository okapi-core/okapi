/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools;

import org.okapi.agent.dto.AgentQueryRecords;
import org.okapi.agent.dto.QuerySpec;

public class HttpQuerySerializer {

  public static QuerySpec serialize(AgentQueryRecords.HttpQuery query) {
    var method = query.method().name();
    var path = query.path();
    var headers = query.requestHeaders();
    var body = query.requestBody();

    var sb = new StringBuilder();
    sb.append(method).append(" ").append(path).append(" ").append("HTTP/1.1").append("\n");
    headers.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));
    if (body != null && !body.isEmpty()) {
      sb.append("\r\n\r\n").append(body);
    }
    return new QuerySpec(sb.toString());
  }
}
