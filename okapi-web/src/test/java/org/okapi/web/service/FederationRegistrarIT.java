package org.okapi.web.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.data.exceptions.ResourceNotFoundException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.web.common.TestingSession;
import org.okapi.web.dtos.sources.CreateFederatedSourceRequest;
import org.okapi.web.service.datasources.FederatedSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class FederationRegistrarIT {

  @Autowired FederatedSourceService registrar;
  @Autowired TestingSession testingSession;

  @BeforeEach
  void setUp() throws Exception {
    testingSession.createIfRequired();
    testingSession.login();
    testingSession.getSessionToken();
  }

  @Test
  public void testRegistrationFlow()
      throws UnAuthorizedException, BadRequestException, ResourceNotFoundException {
    var tempToken = testingSession.getTempToken();
    CreateFederatedSourceRequest request =
        CreateFederatedSourceRequest.builder()
            .sourceName("test-source")
            .sourceType("PROMETHEUS")
            .build();
    var response = registrar.create(ProtectedResourceContext.of(tempToken.getToken()), request);
    var allSources = registrar.listAll(tempToken.getToken());
    Assertions.assertEquals(1, allSources.size());
    var fetchedSource =
        registrar.read(ProtectedResourceContext.of(tempToken.getToken(), "test-source"));
    Assertions.assertEquals(response.getSourceName(), fetchedSource.getSourceName());
    Assertions.assertEquals(response.getSourceType(), fetchedSource.getSourceType());

    registrar.delete(
        ProtectedResourceContext.of(tempToken.getToken(), fetchedSource.getSourceName()));
    var sourcesAfterDeletion = registrar.listAll(tempToken.getToken());
    Assertions.assertEquals(0, sourcesAfterDeletion.size());
  }
}
