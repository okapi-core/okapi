/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.fakes;

import java.util.ArrayList;
import java.util.List;
import org.okapi.web.ai.provider.AiProvider;
import org.okapi.web.ai.provider.ApiRequest;
import org.okapi.web.ai.provider.ApiResponse;

public class FakeAiProvider implements AiProvider {
  int counter = 0;
  List<ApiResponse> responses = new ArrayList<>();
  List<ApiRequest> requests = new ArrayList<>();

  public void addResponse(ApiResponse response) {
    responses.add(response);
  }

  public List<ApiRequest> getRequests() {
    return requests;
  }

  @Override
  public ApiResponse getResponse(ApiRequest request) {
    requests.add(request);
    if (counter < responses.size()) {
      return responses.get(counter++);
    }
    throw new RuntimeException("No more fake responses available");
  }
}
