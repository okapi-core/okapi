/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.runtime;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractio.BinFilesPrefixRegistry;
import org.okapi.abstractio.DiskLogBinPaths;
import org.okapi.exceptions.ExceptionUtils;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
public class AbstractS3Uploader<Id> extends GenericS3Uploader<Id> {
  public AbstractS3Uploader(
      String bucket,
      String basePrefix,
      long expiryDurationMs,
      S3Client s3Client,
      DiskLogBinPaths<Id> binPaths,
      BinFilesPrefixRegistry binFilesPrefixRegistry,
      String partName) {
    super(
        bucket, basePrefix, expiryDurationMs, s3Client, binPaths, binFilesPrefixRegistry, partName);
  }

  public void uploadBlockNoThrow() {
    try {
      uploadBlock();
    } catch (IOException e) {
      log.error("Error uploading block to S3", e);
      log.error("Uploading failed due to {}", ExceptionUtils.debugFriendlyMsg(e));
    }
  }
}
