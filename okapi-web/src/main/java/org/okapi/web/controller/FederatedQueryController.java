/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.controller;

import org.okapi.web.dtos.federation.FederatedQueryRequest;
import org.okapi.web.dtos.federation.FederatedQueryResponse;
import org.okapi.web.headers.RequestHeaders;
import org.okapi.web.service.federation.FederatedQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/federated")
public class FederatedQueryController {

  @Autowired FederatedQueryService federatedQueryService;

  @GetMapping("/query")
  public FederatedQueryResponse getFederatedQueryResponse(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @RequestBody FederatedQueryRequest queryRequest)
      throws Exception {
    return federatedQueryService.query(tempToken, queryRequest);
  }
}
