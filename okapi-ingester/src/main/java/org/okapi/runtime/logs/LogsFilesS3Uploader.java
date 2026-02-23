/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.runtime.logs;

import org.okapi.abstractio.BinFilesPrefixRegistry;
import org.okapi.abstractio.PartNames;
import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.paths.LogsDiskPaths;
import org.okapi.runtime.AbstractS3Uploader;
import software.amazon.awssdk.services.s3.S3Client;

public class LogsFilesS3Uploader extends AbstractS3Uploader<String> {
  public LogsFilesS3Uploader(
      LogsCfg logsCfg,
      S3Client s3Client,
      LogsDiskPaths logsDiskPaths,
      BinFilesPrefixRegistry binFilesPrefixRegistry) {
    super(
        logsCfg.getS3Bucket(),
        logsCfg.getS3BasePrefix(),
        logsCfg.getIdxExpiryDuration(),
        s3Client,
        logsDiskPaths,
        binFilesPrefixRegistry,
        PartNames.LOG_FILE_PART);
  }
}
