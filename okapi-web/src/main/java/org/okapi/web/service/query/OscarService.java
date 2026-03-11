/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.query;

import lombok.RequiredArgsConstructor;
import org.okapi.oscar.client.OscarClient;
import org.okapi.rest.chat.ChatHistoryResponse;
import org.okapi.rest.chat.ChatMessageUpdatesResponse;
import org.okapi.rest.chat.ChatResponse;
import org.okapi.rest.chat.GetHistoryRequest;
import org.okapi.rest.chat.PostMessageRequest;
import org.okapi.rest.session.SessionMetaResponse;
import org.okapi.web.service.access.OrgMemberChecker;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OscarService {

  private final OscarClient oscarClient;
  private final OrgMemberChecker orgMemberChecker;

  public ChatResponse postMessage(String token, String sessionId, PostMessageRequest request) {
    orgMemberChecker.checkUserIsOrgMember(token);
    return oscarClient.postMessage(sessionId, request);
  }

  public ChatHistoryResponse getHistory(String token, String sessionId, GetHistoryRequest request) {
    orgMemberChecker.checkUserIsOrgMember(token);
    return oscarClient.getHistory(sessionId, request);
  }

  public ChatMessageUpdatesResponse getUpdates(String token, String sessionId) {
    orgMemberChecker.checkUserIsOrgMember(token);
    return oscarClient.getUpdates(sessionId);
  }

  public SessionMetaResponse createSession(String token) {
    orgMemberChecker.checkUserIsOrgMember(token);
    return oscarClient.createSession();
  }

  public SessionMetaResponse getSessionMeta(String token, String sessionId) {
    orgMemberChecker.checkUserIsOrgMember(token);
    return oscarClient.getSessionMeta(sessionId);
  }

  public SessionMetaResponse pingSession(String token, String sessionId) {
    orgMemberChecker.checkUserIsOrgMember(token);
    return oscarClient.pingSession(sessionId);
  }
}
