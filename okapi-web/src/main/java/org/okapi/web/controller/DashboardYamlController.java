package org.okapi.web.controller;

import org.okapi.web.dtos.dashboards.yaml.ApplyDashboardYamlRequest;
import org.okapi.web.dtos.dashboards.yaml.ApplyDashboardYamlResponse;
import org.okapi.web.dtos.dashboards.yaml.LintDashboardYamlRequest;
import org.okapi.web.dtos.dashboards.yaml.LintDashboardYamlResponse;
import org.okapi.web.headers.RequestHeaders;
import org.okapi.web.service.dashboards.DashboardYamlIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboards/yaml")
public class DashboardYamlController {
  @Autowired DashboardYamlIngestionService ingestionService;

  @PostMapping("/lint")
  public LintDashboardYamlResponse lint(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @RequestBody @Validated LintDashboardYamlRequest req)
      throws Exception {
    return ingestionService.lint(tempToken, req);
  }

  @PostMapping("/apply")
  public ApplyDashboardYamlResponse apply(
      @RequestHeader(RequestHeaders.TEMP_TOKEN) String tempToken,
      @RequestBody @Validated ApplyDashboardYamlRequest req)
      throws Exception {
    return ingestionService.apply(tempToken, req);
  }
}
