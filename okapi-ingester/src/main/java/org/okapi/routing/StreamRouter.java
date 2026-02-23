/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.routing;

import org.okapi.streams.StreamIdentifier;

public interface StreamRouter<Id> {

  String getNodesForReading(StreamIdentifier<Id> streamIdentifier, long idxBlock);
}
