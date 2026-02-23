/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation;

import java.util.function.Function;
import org.okapi.json.JsonExtractor;
import org.okapi.web.ai.provider.ApiRequest;
import org.okapi.web.ai.provider.ApiResponse;
import org.okapi.web.investigation.exception.UnexpectedModelResponseException;

public abstract class AbstractAgentLoop<T> {
  int maxDepth;
  int currentDepth = 0;

  public AbstractAgentLoop(int maxDepth) {
    this.maxDepth = maxDepth;
    this.currentDepth = 0;
  }

  String[] ACTION_PATH = {"action"};

  public abstract ApiResponse nextResponse(ApiRequest request);

  public abstract boolean shouldTerminate(ApiResponse response)
      throws UnexpectedModelResponseException;

  public abstract ApiRequest nextRequest(int iteration, ApiRequest prev, ApiResponse response)
      throws RuntimeException;

  public abstract Function<ApiResponse, T> getFinalResponseConverter();

  public abstract Function<ApiResponse, Void> getPartialListener();

  public T agentLoop() throws AgentDidNotFinishException {
    var request = getInitialRequest();
    var converter = getFinalResponseConverter();
    var partial = getPartialListener();
    while (this.currentDepth++ < maxDepth) {
      var response = nextResponse(request);
      if (shouldTerminate(response)) {
        return converter.apply(response);
      } else {
        partial.apply(response);
        request = nextRequest(this.currentDepth, request, response);
      }
    }
    throw new AgentDidNotFinishException("Maximum depth reached, agent did not finish");
  }

  public String getAction(JsonExtractor extractor) {
    var action = extractor.getAsString(ACTION_PATH);
    if (action.isEmpty()) {
      throw new UnexpectedModelResponseException("No action found in model response");
    }
    return action.get();
  }

  public abstract ApiRequest getInitialRequest();
}
