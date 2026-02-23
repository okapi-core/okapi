/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.federation.locator;

import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.NotFoundException;
import org.okapi.web.service.federation.FederatedAgent;

public interface AgentLocator {
  FederatedAgent getAgent(String tenant, String source)
      throws BadRequestException, NotFoundException;
}
