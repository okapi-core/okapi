/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.abstractio;

public interface TimeRangeSnapshot {

  long getTsStart();

  long getTsEnd();

  int getNDocs();
}
