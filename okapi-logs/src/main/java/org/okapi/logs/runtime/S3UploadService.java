package org.okapi.logs.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.logs.config.LogsConfigProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3UploadService {
  private final LogsConfigProperties cfg;
  private final S3Client s3Client;
  private final NodeIdSupplier nodeIdSupplier;

  public void uploadHour(long hour) throws IOException {
    if (!cfg.isS3UploadEnabled()) return;
    if (cfg.getS3Bucket() == null || cfg.getS3Bucket().isEmpty()) return;
    Path base = Path.of(cfg.getDataDir());
    if (!Files.exists(base)) return;
    Files.walk(base, 4)
        .filter(Files::isDirectory)
        .filter(p -> p.getNameCount() >= base.getNameCount() + 3)
        .forEach(
            hourDir -> {
              try {
                var h = Integer.parseInt(hourDir.getFileName().toString());
                if (h != hour) return;
              } catch (NumberFormatException nfe) {
                return;
              }
              try {
                Path streamDir = hourDir.getParent();
                Path tenantDir = streamDir.getParent();
                String tenant = tenantDir.getFileName().toString();
                String stream = streamDir.getFileName().toString();
                uploadHourDir(tenant, stream, hour, hourDir);
              } catch (Exception e) {
                log.warn("Failed to upload {}", hourDir, e);
              }
            });
  }

  private void uploadHourDir(String tenant, String stream, long hour, Path hourDir)
      throws IOException {
    Path bin = hourDir.resolve("logfile.bin");
    Path idx = hourDir.resolve("logfile.idx");
    if (!Files.exists(bin) || !Files.exists(idx)) return;

    String basePrefix = (cfg.getS3BasePrefix() == null ? "logs" : cfg.getS3BasePrefix());
    String nodeSeg = cfg.isS3UploadIncludeNodeId() ? "/" + nodeIdSupplier.getNodeId() : "";
    String prefix = basePrefix + "/" + tenant + "/" + stream + "/" + hour + nodeSeg;

    s3Client.putObject(
        PutObjectRequest.builder().bucket(cfg.getS3Bucket()).key(prefix + "/logfile.idx").build(),
        RequestBody.fromBytes(Files.readAllBytes(idx)));
    s3Client.putObject(
        PutObjectRequest.builder().bucket(cfg.getS3Bucket()).key(prefix + "/logfile.bin").build(),
        RequestBody.fromBytes(Files.readAllBytes(bin)));
  }
}

