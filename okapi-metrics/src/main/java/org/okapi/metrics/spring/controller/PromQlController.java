package org.okapi.metrics.spring.controller;

import com.google.gson.Gson;
import java.util.List;
import org.okapi.beans.Configurations;
import org.okapi.exceptions.BadRequestException;
import org.okapi.headers.CookiesAndHeaders;
import org.okapi.metrics.query.promql.PromQlQueryProcessor;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1")
@RestController
public class PromQlController {

  @Autowired
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

  @GetMapping("/query_range")
  public ResponseEntity<String> queryRange(
      @RequestHeader(CookiesAndHeaders.HEADER_OKAPI_TENANT) String tenant,
      @RequestParam("query") String query,
      @RequestParam("start") String start,
      @RequestParam("end") String end,
      @RequestParam("step") String step,
      @RequestParam(value = "timeout", required = false) String timeout)
      throws BadRequestException, EvaluationException {
    var result = promQlQueryProcessor.queryRangeApi(tenant, query, start, end, step);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }

  @GetMapping("/labels")
  public ResponseEntity<String> listLabels(
      @RequestParam(value = "start", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          String start,
      @RequestParam(value = "end", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          String end,
      @RequestParam(value = "match[]", required = false) List<String> matchers,
      @RequestHeader(CookiesAndHeaders.HEADER_OKAPI_TENANT) String tenantId)
      throws BadRequestException {
    var result = promQlQueryProcessor.queryMatchApi(tenantId, matchers, start, end);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }

  @GetMapping("/metadata")
  public ResponseEntity<String> getMetadata(
      @RequestParam(value = "start", required = false) String start,
      @RequestParam(value = "end", required = false) String end,
      @RequestParam(value = "match[]", required = false) List<String> matchers,
      @RequestHeader(CookiesAndHeaders.HEADER_OKAPI_TENANT) String tenantId) {

    return ResponseEntity.ok("");
  }

  @GetMapping("/label/{label}/values")
  public ResponseEntity<String> listLabels(
      @PathVariable("label") String label,
      @RequestParam(value = "start", required = false) String start,
      @RequestParam(value = "end", required = false) String end,
      @RequestParam(value = "match[]", required = false) List<String> matchers,
      @RequestHeader(CookiesAndHeaders.HEADER_OKAPI_TENANT) String tenantId)
      throws BadRequestException {
    var result = promQlQueryProcessor.queryLabelsApi(tenantId, label, matchers, start, end);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }
}
