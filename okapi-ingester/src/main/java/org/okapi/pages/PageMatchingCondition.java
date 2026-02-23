/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import org.okapi.streams.StreamIdentifier;

public interface PageMatchingCondition {
  boolean isMatch(StreamIdentifier streamIdentifier, long block);
}
