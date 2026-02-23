/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AiSreSession {
  String orgId;

  public String orgId() {
    return orgId;
  }
}
