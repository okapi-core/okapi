package org.okapi.promql.api;

import com.google.gson.Gson;
import java.util.List;
import org.okapi.Constants;
import org.okapi.beans.Configurations;
import org.okapi.exceptions.BadRequestException;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.query.PromQlMetadataService;
import org.okapi.promql.query.PromQlQueryProcessor;
import org.okapi.rest.promql.GetPromQlResponse;
import org.okapi.rest.promql.PromQlMetadataItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1")
@RestController
public class PromQlController {

  @Autowired
  @Qualifier(Configurations.BEAN_PROMQL_SERIALIZER)
  Gson promQlSerializer;

  @Autowired PromQlQueryProcessor promQlQueryProcessor;
  @Autowired PromQlMetadataService promQlMetadataService;

  @GetMapping(value = "/query", produces = "application/json")
  public ResponseEntity<String> queryGet(
      @RequestParam("query") String query,
      @RequestParam(value = "time", required = false) String time,
      @RequestParam(value = "timeout", required = false) String timeout)
      throws BadRequestException, EvaluationException {
    var result = promQlQueryProcessor.queryInstantApi(Constants.DEFAULT_TENANT, query, time);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }

  @PostMapping(
      value = "/query",
      consumes = "application/x-www-form-urlencoded",
      produces = "application/json")
  public ResponseEntity<String> queryPost(
      @RequestParam("query") String query,
      @RequestParam(value = "time", required = false) String time,
      @RequestParam(value = "timeout", required = false) String timeout)
      throws BadRequestException, EvaluationException {
    var result = promQlQueryProcessor.queryInstantApi(Constants.DEFAULT_TENANT, query, time);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }

  @GetMapping("/query_range")
  public ResponseEntity<String> queryRange(
      @RequestParam("query") String query,
      @RequestParam("start") String start,
      @RequestParam("end") String end,
      @RequestParam("step") String step,
      @RequestParam(value = "timeout", required = false) String timeout)
      throws BadRequestException, EvaluationException {
    var result =
        promQlQueryProcessor.queryRangeApi(Constants.DEFAULT_TENANT, query, start, end, step);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }

  @PostMapping(
      value = "/query_range",
      consumes = "application/x-www-form-urlencoded",
      produces = "application/json")
  public ResponseEntity<String> queryRangePost(
      @RequestParam("query") String query,
      @RequestParam("start") String start,
      @RequestParam("end") String end,
      @RequestParam("step") String step,
      @RequestParam(value = "timeout", required = false) String timeout)
      throws BadRequestException, EvaluationException {
    var result =
        promQlQueryProcessor.queryRangeApi(Constants.DEFAULT_TENANT, query, start, end, step);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }

  @GetMapping("/labels")
  public ResponseEntity<String> listLabels(
      @RequestParam(value = "start", required = false) String start,
      @RequestParam(value = "end", required = false) String end,
      @RequestParam(value = "match[]", required = false) String matchers)
      throws BadRequestException {
    var result =
        promQlQueryProcessor.queryLabelNamesApi(Constants.DEFAULT_TENANT, List.of(), start, end);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }

  @GetMapping("/label/{label}/values")
  public ResponseEntity<String> listLabelValues(
      @PathVariable("label") String label,
      @RequestParam(value = "start", required = false) String start,
      @RequestParam(value = "end", required = false) String end,
      @RequestParam(value = "match[]", required = false) String matchers)
      throws BadRequestException {
    var result =
        promQlQueryProcessor.queryLabelsApi(Constants.DEFAULT_TENANT, label, List.of(), start, end);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }

  @GetMapping("/metadata")
  public ResponseEntity<String> metadata(
      @RequestParam(value = "metric", required = false) String metric,
      @RequestParam(value = "limit", required = false) Integer limit) {
    GetPromQlResponse<java.util.Map<String, java.util.List<PromQlMetadataItem>>> result =
        promQlMetadataService.getMetadata(metric, limit);
    var responseBody = promQlSerializer.toJson(result);
    return ResponseEntity.ok(responseBody);
  }
}
