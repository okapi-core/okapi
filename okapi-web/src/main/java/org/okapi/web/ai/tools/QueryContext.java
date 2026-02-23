/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class QueryContext {
  String sourceId;
  AiSreSession session;

  public String sourceId() {
    return sourceId;
  }
}
