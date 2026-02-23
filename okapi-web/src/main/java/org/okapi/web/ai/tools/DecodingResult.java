/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools;

public class DecodingResult<T> {
  private final T result;
  private final boolean success;
  private final String errorMessage;

  public DecodingResult(T result) {
    this.result = result;
    this.success = true;
    this.errorMessage = null;
  }

  public DecodingResult(String errorMessage) {
    this.result = null;
    this.success = false;
    this.errorMessage = errorMessage;
  }

  public T getResult() {
    return result;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
