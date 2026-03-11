package org.okapi.oscar.controller;

import org.okapi.oscar.service.OscarAi;
import org.okapi.rest.session.SessionMetaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

  @Autowired OscarAi oscarAi;

  @PostMapping
  public SessionMetaResponse createSession() {
    return oscarAi.createSession();
  }

  @GetMapping("/{sessionId}/meta")
  public SessionMetaResponse getSessionMeta(@PathVariable("sessionId") String sessionId) {
    return oscarAi.getSessionMeta(sessionId);
  }

  @PostMapping("/{sessionId}/ping")
  public SessionMetaResponse ping(@PathVariable("sessionId") String sessionId) {
    return oscarAi.pingSession(sessionId);
  }
}
