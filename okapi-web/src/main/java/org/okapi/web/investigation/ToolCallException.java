/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation;

import lombok.experimental.StandardException;

@StandardException
public class ToolCallException extends Exception {
  public ToolCallException(String message) {
    super(message);
  }
}
