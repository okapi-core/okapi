/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.query;

import java.util.Optional;

public class HttpHeaderLineDeserializer {

  // headers are specified by standard Http RFC 7230 as
  // a series of key:value pairs separated by CRLF
  // e.g.
  // Header1: value1
  public static HttpHeaderLineDeserializer create() {
    return new HttpHeaderLineDeserializer();
  }

  public Optional<String[]> deserializeHeaderLine(String serializedHeaders) {
    // parsed header line
    var split = serializedHeaders.split(":", 2);
    if (split.length != 2) {
      return Optional.empty();
    }
    return Optional.of(new String[] {split[0].trim(), split[1].trim()});
  }
}
