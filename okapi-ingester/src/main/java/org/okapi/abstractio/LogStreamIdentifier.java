/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.abstractio;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.okapi.streams.StreamIdentifier;

@EqualsAndHashCode
@Getter
public class LogStreamIdentifier implements StreamIdentifier<String> {
  String streamId;

  public LogStreamIdentifier(String streamId) {
    this.streamId = streamId;
  }

  public static LogStreamIdentifier of(String streamId) {
    return new LogStreamIdentifier(streamId);
  }
}
