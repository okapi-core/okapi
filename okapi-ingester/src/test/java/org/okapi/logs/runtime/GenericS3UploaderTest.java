/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.runtime;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.okapi.abstractio.BinFilesPrefixRegistry;
import org.okapi.abstractio.DiskLogBinPaths;
import org.okapi.runtime.GenericS3Uploader;
import org.okapi.spring.configs.properties.LogsCfgImpl;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class GenericS3UploaderTest {

  @TempDir Path tempDir;
  @Mock S3Client s3Client;
  @Mock DiskLogBinPaths binPaths;
  @Mock BinFilesPrefixRegistry prefixRegistry;
  LogsCfgImpl cfg;

  GenericS3Uploader svc;

  long expiredHour;
  long freshHour;
  Path pathA;
  Path pathB;

  @BeforeEach
  void setup() throws IOException {
    cfg = new LogsCfgImpl();
    cfg.setS3Bucket("temp-bucket");
    cfg.setDataDir(tempDir.toString());
    cfg.setS3Bucket("bkt");
    cfg.setS3BasePrefix("logs");
    cfg.setIdxExpiryDuration(3600_000); // 1 hour
    expiredHour = (System.currentTimeMillis() / cfg.getIdxExpiryDuration()) - 2;
    freshHour = (System.currentTimeMillis() / cfg.getIdxExpiryDuration());
    pathA =
        tempDir
            .resolve("tA")
            .resolve("sA")
            .resolve(Long.toString(expiredHour))
            .resolve("logfile.bin");
    Files.createDirectories(pathA.getParent());
    Files.writeString(pathA, "BIN A");
    pathB =
        tempDir
            .resolve("tB")
            .resolve("sb")
            .resolve(Long.toString(freshHour))
            .resolve("logfile.bin");
    Files.createDirectories(pathB.getParent());
    Files.writeString(pathB, "BIN B");
    svc =
        new GenericS3Uploader(
            cfg.getS3Bucket(),
            cfg.getS3BasePrefix(),
            cfg.getIdxExpiryDuration(),
            s3Client,
            binPaths,
            prefixRegistry,
            "logfile.bin");
  }

  @Test
  void uploadsAllExpiredFiles() throws IOException {
    when(binPaths.listAllPaths())
        .thenReturn(
            java.util.List.of(
                new DiskLogBinPaths.TimestampedBinFile(
                    "tA", "sA", pathA, expiredHour, expiredHour * cfg.getIdxExpiryDuration()),
                new DiskLogBinPaths.TimestampedBinFile(
                    "tB", "sb", pathB, freshHour, freshHour * cfg.getIdxExpiryDuration())));
    when(prefixRegistry.getRootPrefixForLogBinFileForMe(
            cfg.getS3BasePrefix(), "tA", "sA", "logfile.bin", expiredHour))
        .thenReturn("logs/tA/sA/node-1/" + expiredHour + "/logfile.bin");
    svc.uploadBlock();
    verify(s3Client, times(1))
        .putObject(
            eq(
                PutObjectRequest.builder()
                    .bucket("bkt")
                    .key("logs/tA/sA/node-1/" + expiredHour + "/logfile.bin")
                    .build()),
            eq(pathA));
    Assertions.assertTrue(svc.isAlreadyUploaded(pathA));
    verify(s3Client, times(0))
        .putObject(
            eq(
                PutObjectRequest.builder()
                    .bucket("bkt")
                    .key("logs/tB/sb/node-1/" + freshHour + "/logfile.bin")
                    .build()),
            eq(pathB));
    Assertions.assertFalse(svc.isAlreadyUploaded(pathB));
  }

  @Test
  void testWriteAcknowledgement() throws IOException {
    Assertions.assertFalse(svc.isAlreadyUploaded(pathA));
    svc.writeAcknowledgement(pathA);
    Assertions.assertTrue(svc.isAlreadyUploaded(pathA));
  }

  @Test
  void testAckIsInvalidatedOnContentUpdate() throws IOException {
    svc.writeAcknowledgement(pathA);
    Assertions.assertTrue(svc.isAlreadyUploaded(pathA));
    Files.write(pathA, "updated content".getBytes());
    Assertions.assertFalse(svc.isAlreadyUploaded(pathA));
    svc.writeAcknowledgement(pathA);
    Assertions.assertTrue(svc.isAlreadyUploaded(pathA));
  }
}
