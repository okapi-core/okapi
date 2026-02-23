/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractio.BinFilesPrefixRegistry;
import org.okapi.abstractio.DiskLogBinPaths;
import org.okapi.checksums.ChecksumUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
public class GenericS3Uploader<Id> {
  public final String partName;
  private final String s3Bucket;
  private final String basePrefix;
  private final Long expiryDurationMs;
  private final S3Client s3Client;
  private final DiskLogBinPaths<Id> binPaths;
  private final BinFilesPrefixRegistry binFilesPrefixRegistry;

  public GenericS3Uploader(
      String bucket,
      String basePrefix,
      long expiryDurationMs,
      S3Client s3Client,
      DiskLogBinPaths<Id> binPaths,
      BinFilesPrefixRegistry binFilesPrefixRegistry,
      String partName) {
    this.expiryDurationMs = expiryDurationMs;
    this.s3Client = s3Client;
    this.binPaths = binPaths;
    this.binFilesPrefixRegistry = binFilesPrefixRegistry;
    this.s3Bucket = bucket;
    this.basePrefix = basePrefix;
    this.partName = partName;
  }

  public void uploadBlock() throws IOException {
    var blocks = binPaths.listAllPaths();
    for (var blk : blocks) {
      var currentTimeBlock = System.currentTimeMillis() / expiryDurationMs;
      var isExpired = currentTimeBlock > blk.blk();
      if (isExpired) {
        uploadFile(blk);
      }
    }
  }

  public void uploadAll() throws IOException {
    var blocks = binPaths.listAllPaths();
    for (var blk : blocks) {
      uploadFile(blk);
    }
  }

  public void uploadFilesMatchingCondition(
      Function<DiskLogBinPaths.TimestampedBinFile, Boolean> predicate) throws IOException {
    var allFiles = binPaths.listAllPaths();
    for (var file : allFiles) {
      if (predicate.apply(file)) {
        uploadFile(file);
      }
    }
  }

  public void uploadFile(DiskLogBinPaths.TimestampedBinFile binFileInfo) throws IOException {
    var path = binFileInfo.path();
    if (!Files.exists(path)) {
      return;
    }
    var blk = binFileInfo.timestamp() / expiryDurationMs;
    var prefix =
        binFilesPrefixRegistry.getRootPrefixForLogBinFileForMe(
            basePrefix, binFileInfo.tenant(), binFileInfo.stream(), partName, blk);
    if (isAlreadyUploaded(path)) {
      return;
    }
    s3Client.putObject(PutObjectRequest.builder().bucket(s3Bucket).key(prefix).build(), path);
    writeAcknowledgement(path);
  }

  public void writeAcknowledgement(Path path) throws IOException {
    var ackPath = getAckPath(path);
    var checkSum = ChecksumUtils.getChecksum(path);
    Files.writeString(ackPath, checkSum);
  }

  public Path getAckPath(Path path) {
    var ackFile = path.toAbsolutePath() + ".ack";
    return Path.of(ackFile);
  }

  public boolean isAlreadyUploaded(Path path) throws IOException {
    var checkSum = ChecksumUtils.getChecksum(path);
    var ackPath = getAckPath(path);
    if (!Files.exists(ackPath)) return false;
    var checkSumOnFile = Files.readString(ackPath);
    return checkSum.equals(checkSumOnFile);
  }
}
