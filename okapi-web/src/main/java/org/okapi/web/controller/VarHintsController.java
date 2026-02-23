package org.okapi.web.controller;

import org.okapi.web.dtos.dashboards.vars.GetVarHintsRequest;
import org.okapi.web.dtos.dashboards.vars.VarHintsResponse;
import org.okapi.web.headers.RequestHeaders;
import org.okapi.web.service.dashboards.hints.VariableHintsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class VarHintsController {
  @Autowired VariableHintsService variableHintsService;

  @PostMapping("/variable-hints")
  public VarHintsResponse getVarHints(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String token,
      @RequestBody @Validated GetVarHintsRequest request) {
    return variableHintsService.getVarHints(token, request);
  }
}
