/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class RoleAndPrompt {
  String role;
  String prompt;

  public static RoleAndPrompt withPrompt(String newPrompt) {
    return new RoleAndPrompt("user", newPrompt);
  }
}
