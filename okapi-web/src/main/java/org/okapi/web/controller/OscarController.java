/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.controller;

import lombok.AllArgsConstructor;
import org.okapi.headers.CookiesAndHeaders;
import org.okapi.rest.chat.ChatHistoryResponse;
import org.okapi.rest.chat.ChatMessageUpdatesResponse;
import org.okapi.rest.chat.ChatResponse;
import org.okapi.rest.chat.GetHistoryRequest;
import org.okapi.rest.chat.PostMessageRequest;
import org.okapi.web.service.query.OscarService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@AllArgsConstructor
public class OscarController {

  OscarService oscarService;

  @PostMapping("/{sessionId}")
  public ChatResponse postMessage(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @PathVariable("sessionId") String sessionId,
      @RequestBody @Validated PostMessageRequest request) {
    return oscarService.postMessage(tempToken, sessionId, request);
  }

  @PostMapping("/messages/{sessionId}")
  public ChatHistoryResponse getHistory(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @PathVariable("sessionId") String sessionId,
      @RequestBody @Validated GetHistoryRequest request) {
    return oscarService.getHistory(tempToken, sessionId, request);
  }

  @GetMapping("/{sessionId}/updates")
  public ChatMessageUpdatesResponse getUpdates(
      @RequestHeader(CookiesAndHeaders.HEADER_TEMP_TOKEN) String tempToken,
      @PathVariable("sessionId") String sessionId) {
    return oscarService.getUpdates(tempToken, sessionId);
  }
}
