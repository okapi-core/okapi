package org.okapi.traces.api;

import org.okapi.rest.traces.red.ListServicesRequest;
import org.okapi.rest.traces.red.ServiceListResponse;
import org.okapi.rest.traces.red.ServiceRedRequest;
import org.okapi.rest.traces.red.ServiceRedResponse;
import org.okapi.traces.ch.reds.ChRedQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ChRedsController {
  @Autowired private ChRedQueryService redQueryService;

  @PostMapping("/reds")
  public ServiceRedResponse getReds(@Validated @RequestBody ServiceRedRequest request) {
    return redQueryService.queryRed(request);
  }

  @PostMapping("/services")
  public ServiceListResponse getServices(@Validated @RequestBody ListServicesRequest request) {
    return redQueryService.queryServiceList(request);
  }
}
