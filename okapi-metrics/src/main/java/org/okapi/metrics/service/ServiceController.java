/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.service;

public interface ServiceController {
  boolean canConsume();

  void pauseConsumer();

  boolean resumeConsumer();

  boolean startProcess();

  void stopProcess();

  boolean isProcessRunning();

  boolean isBoxEmpty();
}
