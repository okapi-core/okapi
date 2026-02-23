package org.okapi.web.controller;

import java.util.List;
import org.okapi.data.exceptions.ResourceNotFoundException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.UnAuthorizedException;
import org.okapi.web.dtos.sources.CreateFederatedSourceRequest;
import org.okapi.web.dtos.sources.GetFederatedSourceResponse;
import org.okapi.web.headers.RequestHeaders;
import org.okapi.web.service.ProtectedResourceContext;
import org.okapi.web.service.datasources.FederatedSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/federated-sources")
public class FederatedSourceController {

  @Autowired FederatedSourceService federationRegistrar;

  @PostMapping("/register")
  public GetFederatedSourceResponse createFederatedSource(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @RequestBody CreateFederatedSourceRequest request)
      throws BadRequestException, UnAuthorizedException, ResourceNotFoundException {
    return federationRegistrar.create(ProtectedResourceContext.of(tempToken), request);
  }

  @PostMapping("/{sourceName}/delete")
  public void deleteFederatedSource(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken, @PathVariable String sourceName)
      throws BadRequestException, ResourceNotFoundException, UnAuthorizedException {
    federationRegistrar.delete(ProtectedResourceContext.of(tempToken, sourceName));
  }

  @GetMapping("")
  public List<GetFederatedSourceResponse> listFederatedSources(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken) throws UnAuthorizedException {
    return federationRegistrar.listAll(tempToken);
  }

  @GetMapping("/{sourceName}")
  public GetFederatedSourceResponse getFederatedSource(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken, @PathVariable String sourceName)
      throws UnAuthorizedException, BadRequestException, ResourceNotFoundException {
    return federationRegistrar.read(ProtectedResourceContext.of(tempToken, sourceName));
  }
}
