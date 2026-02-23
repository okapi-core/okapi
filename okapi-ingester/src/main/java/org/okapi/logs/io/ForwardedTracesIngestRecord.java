/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.io;

import java.util.List;

public class ForwardedTracesIngestRecord {
  int shard;
  List<LogIngestRecord> records;
}
