/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.streams.StreamIdentifier;

@Getter
@AllArgsConstructor
public class MetricsStreamIdentifier implements StreamIdentifier<String> {
  String streamId;
}
