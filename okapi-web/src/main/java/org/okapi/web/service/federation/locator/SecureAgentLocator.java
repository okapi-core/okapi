package org.okapi.web.service.federation.locator;

import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.NotFoundException;
import org.okapi.web.service.federation.FederatedAgent;
import org.springframework.stereotype.Service;

@Service
public class SecureAgentLocator implements AgentLocator{
    @Override
    public FederatedAgent getAgent(String tenant, String source) throws BadRequestException, NotFoundException {
        return null;
    }
}
