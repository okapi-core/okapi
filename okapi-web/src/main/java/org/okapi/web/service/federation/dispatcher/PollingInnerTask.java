/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service.federation.dispatcher;

public interface PollingInnerTask<T> {
  PollingTask.PollStatus<T> run();
}
