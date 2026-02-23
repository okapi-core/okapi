/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MockPageSnapshot {
  List<MockPageInput> inputs;
  long tsStart;
  long tsEnd;
}
