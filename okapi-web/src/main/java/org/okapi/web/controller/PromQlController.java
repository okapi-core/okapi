/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import org.okapi.headers.CookiesAndHeaders;
import org.okapi.web.service.query.PromQlService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class PromQlController {

  PromQlService promQlService;

  @GetMapping("/query")
  public String queryGet(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @RequestParam("query") String query,
      @RequestParam(value = "time", required = false) String time,
      @RequestParam(value = "timeout", required = false) String timeout) {
    return promQlService.queryPromQlInstant(tempToken, query, time, timeout);
  }

  @PostMapping(
      value = "/query",
      consumes = "application/x-www-form-urlencoded",
      produces = "application/json")
  public String queryPost(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @RequestParam("query") String query,
      @RequestParam(value = "time", required = false) String time,
      @RequestParam(value = "timeout", required = false) String timeout) {
    return promQlService.queryPromQlInstantPost(tempToken, query, time, timeout);
  }

  @GetMapping("/query_range")
  public String queryRange(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @RequestParam("query") String query,
      @RequestParam("start") String start,
      @RequestParam("end") String end,
      @RequestParam("step") String step,
      @RequestParam(value = "timeout", required = false) String timeout) {
    return promQlService.queryPromQlRange(tempToken, query, start, end, step, timeout);
  }

  @GetMapping("/labels")
  public String listLabels(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @RequestParam(value = "start", required = false) String start,
      @RequestParam(value = "end", required = false) String end,
      @RequestParam(value = "match[]", required = false) List<String> matchers) {
    return promQlService.queryPromQlLabels(tempToken, start, end, matchers);
  }

  @GetMapping("/label/{label}/values")
  public String listLabelValues(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @PathVariable("label") String label,
      @RequestParam(value = "start", required = false) String start,
      @RequestParam(value = "end", required = false) String end,
      @RequestParam(value = "match[]", required = false) List<String> matchers) {
    return promQlService.queryPromQlLabelValues(tempToken, label, start, end, matchers);
  }
}
