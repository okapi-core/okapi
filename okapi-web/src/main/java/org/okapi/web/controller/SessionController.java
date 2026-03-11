/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.controller;

import lombok.AllArgsConstructor;
import org.okapi.headers.CookiesAndHeaders;
import org.okapi.rest.session.SessionMetaResponse;
import org.okapi.web.service.query.OscarService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
@AllArgsConstructor
public class SessionController {

  OscarService oscarService;

  @PostMapping
  public SessionMetaResponse createSession(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken) {
    return oscarService.createSession(tempToken);
  }

  @GetMapping("/{sessionId}/meta")
  public SessionMetaResponse getSessionMeta(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @PathVariable("sessionId") String sessionId) {
    return oscarService.getSessionMeta(tempToken, sessionId);
  }

  @PostMapping("/{sessionId}/ping")
  public SessionMetaResponse pingSession(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @PathVariable("sessionId") String sessionId) {
    return oscarService.pingSession(tempToken, sessionId);
  }
}
