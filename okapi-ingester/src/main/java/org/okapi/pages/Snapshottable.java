/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import java.util.List;

public interface Snapshottable<Record> {
  List<Record> snapshot();
}
