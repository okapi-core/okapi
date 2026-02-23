/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

public interface MetadataFilter<M> {
  boolean shouldRead(M metadata);
}
