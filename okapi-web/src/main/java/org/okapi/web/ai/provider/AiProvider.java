/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.provider;

public interface AiProvider {
  ApiResponse getResponse(ApiRequest request);
}
