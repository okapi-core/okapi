package org.okapi.web.service.federation.locator;

import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.NotFoundException;
import org.okapi.web.service.federation.FederatedAgent;

public interface AgentLocator {
  FederatedAgent getAgent(String tenant, String source) throws BadRequestException, NotFoundException;
}
