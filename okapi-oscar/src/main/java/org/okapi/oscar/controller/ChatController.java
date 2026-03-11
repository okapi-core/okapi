package org.okapi.oscar.controller;

import org.okapi.rest.chat.ChatHistoryResponse;
import org.okapi.rest.chat.ChatResponse;
import org.okapi.rest.chat.PostMessageRequest;
import org.okapi.oscar.service.OscarAi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

  @Autowired OscarAi oscarAi;

  @PostMapping("/{sessionId}")
  public ChatResponse postMsg(@PathVariable("sessionId") String sessionId, @RequestBody @Validated PostMessageRequest request) {
    return oscarAi.postMessage(sessionId, request);
  }

  @GetMapping("/messages/{sessionId}")
  public ChatHistoryResponse getHistory(
      @PathVariable("sessionId") String sessionId,
      @RequestParam(value = "from") Long from,
      @RequestParam(value = "to", required = false) Long to) {
    return oscarAi.getHistory(sessionId, from, to);
  }
}
