/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.connection;

public class HttpPathMatcher {
  public boolean matches(String pathPattern, String requestPath) {
    // Simple wildcard matching: '*' matches any sequence of characters
    String regex = pathPattern.replace("*", ".*");
    return requestPath.matches(regex);
  }
}
