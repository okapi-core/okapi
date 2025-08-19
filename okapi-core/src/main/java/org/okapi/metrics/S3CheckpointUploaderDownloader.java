package org.okapi.metrics;

import static org.okapi.metrics.MetadataFields.*;
import static org.okapi.metrics.s3.S3Prefixes.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.okapi.clock.Clock;
import org.okapi.metrics.s3.S3Prefixes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@AllArgsConstructor
public class S3CheckpointUploaderDownloader implements CheckpointUploaderDownloader {
  String dataBucket;
  S3Client amazonS3;
  Clock clock;

  @Override
  public void uploadHourlyCheckpoint(String tenantId, Path path, long epochHourBucket, int shard)
      throws Exception {
    var prefix = hourlyPrefix(tenantId, epochHourBucket, shard);
    var fileSize = Files.size(path);
    if (fileSize == 0) {
      return;
    }

    var checksum = computeMD5(path); // hex string
    var uploadTime = clock.currentTimeMillis();

    amazonS3.putObject(
        PutObjectRequest.builder()
            .bucket(dataBucket)
            .key(prefix)
            // user metadata map (keys are lowercased by S3; read back likewise)
            .metadata(
                Map.of(
                    CHECKSUM_HEADER, checksum,
                    HOURLY_UPLOAD_TIME, Long.toString(uploadTime),
                    OKAPI_TENANT_ID, tenantId))
            .build(),
        RequestBody.fromFile(path));
    Files.deleteIfExists(path);
  }

  @Override
  public void uploadShardCheckpoint(Path path, String opId, int shard) throws IOException {
    var prefix = shardCheckpointPrefix(opId, shard);
    amazonS3.putObject(
        PutObjectRequest.builder().bucket(dataBucket).key(prefix).build(),
        RequestBody.fromFile(path));
  }

  @Override
  public void downloadShardCheckpoint(String opId, int shard, Path path) throws IOException {
    var prefix = shardCheckpointPrefix(opId, shard);

    // doesObjectExist equivalent: HEAD the object and catch 404
    boolean exists;
    try {
      amazonS3.headObject(HeadObjectRequest.builder().bucket(dataBucket).key(prefix).build());
      exists = true;
    } catch (S3Exception e) {
      if (e.statusCode() == 404) {
        exists = false;
      } else {
        throw e;
      }
    }
    if (!exists) return;

    Files.createDirectories(path.getParent());
    amazonS3.getObject(
        GetObjectRequest.builder().bucket(dataBucket).key(prefix).build(),
        ResponseTransformer.toFile(path));
  }

  @Override
  public void uploadParquetDump(String tenantId, Path path, long epochHour) throws IOException {
    var prefix = S3Prefixes.parquetPrefix(tenantId, epochHour);
    amazonS3.putObject(
        PutObjectRequest.builder().bucket(dataBucket).key(prefix).build(),
        RequestBody.fromFile(path));
    Files.deleteIfExists(path);
  }

  public static String computeMD5(Path path) throws Exception {
    MessageDigest md = MessageDigest.getInstance("MD5");
    try (FileInputStream fis = new FileInputStream(path.toFile())) {
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) != -1) {
        md.update(buffer, 0, bytesRead);
      }
    }
    return HexFormat.of().formatHex(md.digest());
  }
}
