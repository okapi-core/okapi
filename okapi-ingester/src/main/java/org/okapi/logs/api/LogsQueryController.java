/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.logs.service.LogsQueryService;
import org.okapi.rest.logs.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Profile("logs")
public class LogsQueryController {

  @Autowired LogsQueryService logsQueryService;

  @Value("${okapi.logs.max-query-limit:1000}")
  private int maxQueryLimit;

  @PostMapping(
      path = "/logs/query",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public QueryResponse query(
      @RequestHeader("X-Okapi-Log-Stream") String logStream, @RequestBody QueryRequest req)
      throws Exception {
    return logsQueryService.queryAllSources(logStream, req, maxQueryLimit);
  }

  @PostMapping(
      path = "/logs/query/local",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public QueryResponse queryLocal(
      @RequestHeader("X-Okapi-Log-Stream") String logStream, @RequestBody QueryRequest req)
      throws Exception {
    return logsQueryService.queryDiskAndBufferPool(logStream, req, maxQueryLimit);
  }
}
