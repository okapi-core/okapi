/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.config;

public interface LogsCfg {
  int getExpectedInsertions();

  String getDataDir();

  int getMaxPageBytes();

  long getMaxPageWindowMs();

  String getS3Bucket();

  String getS3BasePrefix();

  long getS3UploadGraceMs();

  long getIdxExpiryDuration();

  int getSealedPageCap();

  long getSealedPageTtlMs();

  long getBufferPoolFlushEvalMillis();
}
