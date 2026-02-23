/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.ctx;

public interface UpdatableContext<U> {
  void updateContext(U update);
}
