package org.okapi.metrics.controller;

import com.google.gson.Gson;
import org.okapi.beans.Configurations;
import org.okapi.exceptions.BadRequestException;
import org.okapi.headers.CookiesAndHeaders;
import org.okapi.metrics.query.promql.PromQlQueryProcessor;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1")
@RestController
public class PromQlController {

  @Qualifier(Configurations.BEAN_PROMQL_SERIALIZER)
  Gson promQlSerializer;

  @Autowired PromQlQueryProcessor promQlQueryProcessor;

  @GetMapping(value = "/query", produces = "application/json")
  public ResponseEntity<String> queryGet(
      @RequestHeader(CookiesAndHeaders.HEADER_OKAPI_TENANT) String tenant,
      @RequestParam("query") String query,
      @RequestParam(value = "time", required = false) String time,
      @RequestParam(value = "timeout", required = false) String timeout)
      throws BadRequestException, EvaluationException {
    var result = promQlQueryProcessor.queryInstantApi(tenant, query, time);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }

  @PostMapping(
      value = "/query",
      consumes = "application/x-www-form-urlencoded",
      produces = "application/json")
  public ResponseEntity<String> queryPost(
      @RequestHeader(CookiesAndHeaders.HEADER_OKAPI_TENANT) String tenant,
      @RequestParam("query") String query,
      @RequestParam(value = "time", required = false) String time,
      @RequestParam(value = "timeout", required = false) String timeout)
      throws BadRequestException, EvaluationException {
    var result = promQlQueryProcessor.queryInstantApi(tenant, query, time);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }

  @GetMapping("/api/v1/query_range")
  public ResponseEntity<String> queryRange(
      @RequestHeader(CookiesAndHeaders.HEADER_OKAPI_TENANT) String tenant,
      @RequestParam("query") String query,
      @RequestParam("start") String start,
      @RequestParam("end") String end,
      @RequestParam("step") String step,
      @RequestParam(value = "timeout", required = false) String timeout)
      throws BadRequestException, EvaluationException {
    var result = promQlQueryProcessor.queryRange(tenant, query, start, end, step);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }
}
