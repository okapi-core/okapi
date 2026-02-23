/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.exception;

import lombok.experimental.StandardException;

@StandardException
public class MalformedOutputException extends RuntimeException {
  public MalformedOutputException(String message) {
    super(message);
  }
}
