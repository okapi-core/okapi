/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.auth;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Getter
public class OrgIdAssigner {
  String orgId;

  public OrgIdAssigner(@Value("${orgId}") String orgId) {
    this.orgId = orgId;
  }
}
