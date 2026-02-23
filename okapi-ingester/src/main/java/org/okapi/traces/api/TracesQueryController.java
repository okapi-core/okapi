package org.okapi.traces.api;

import org.okapi.rest.traces.SpanQueryRequest;
import org.okapi.rest.traces.SpanQueryResponse;
import org.okapi.spring.configs.Profiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class TracesQueryController {
  public static final int DEFAULT_QUERY_LIMIT = 1000;

  @Autowired private SpanQueryService spanQueryService;

  @PostMapping(
      value = "/span/query/all",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public SpanQueryResponse queryAll(
      @RequestHeader(value = "X-Okapi-App") String app,
      @RequestBody @Validated SpanQueryRequest reqBody)
      throws Exception {
    return spanQueryService.queryAllSources(app, reqBody, DEFAULT_QUERY_LIMIT);
  }

  @PostMapping(
      value = "/span/query/local",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public SpanQueryResponse queryLocal(
      @RequestHeader("X-Okapi-App") String app, @RequestBody SpanQueryRequest reqBody)
      throws Exception {
    return spanQueryService.queryDiskAndBufferPool(app, reqBody, DEFAULT_QUERY_LIMIT);
  }
}
