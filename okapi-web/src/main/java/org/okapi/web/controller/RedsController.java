package org.okapi.web.controller;

import org.okapi.rest.traces.red.ListServicesRequest;
import org.okapi.rest.traces.red.ServiceListResponse;
import org.okapi.rest.traces.red.ServiceRedRequest;
import org.okapi.rest.traces.red.ServiceRedResponse;
import org.okapi.web.headers.RequestHeaders;
import org.okapi.web.service.query.RedsQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RedsController {

  @Autowired private RedsQueryService redQueryService;

  @PostMapping("/reds")
  public ServiceRedResponse getReds(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String token,
      @Validated @RequestBody ServiceRedRequest request) {
    return redQueryService.getServicesReds(token, request);
  }

  @PostMapping("/services")
  public ServiceListResponse getServices(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String token,
      @Validated @RequestBody ListServicesRequest request) {
    return redQueryService.listServices(token, request);
  }
}
