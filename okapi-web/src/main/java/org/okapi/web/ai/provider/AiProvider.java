package org.okapi.web.ai.provider;

public interface AiProvider {
  ApiResponse getResponse(ApiRequest request);
}
