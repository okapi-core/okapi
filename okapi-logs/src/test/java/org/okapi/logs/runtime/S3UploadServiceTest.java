package org.okapi.logs.runtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.logs.config.ModifiableCfg;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3UploadServiceTest {

  @TempDir Path tempDir;

  @Test
  void uploads_specificHour_whenFilesPresent_andIncludesNodeSegment() throws Exception {
    String tenant = "t";
    String stream = "s";
    var cfg = new ModifiableCfg("temp-bucket");
    cfg.setDataDir(tempDir.toString());
    cfg.setS3Bucket("bkt");
    cfg.setS3BasePrefix("logs");
    long hour = (System.currentTimeMillis() / cfg.getIdxExpiryDuration()) - 1;

    Path dir = tempDir.resolve(tenant).resolve(stream).resolve(Long.toString(hour));
    Files.createDirectories(dir);
    Path idx = dir.resolve("logfile.idx");
    Path bin = dir.resolve("logfile.bin");
    Files.writeString(idx, "IDX");
    Files.writeString(bin, "BIN");

    S3Client s3 = mock(S3Client.class);
    NodeIdSupplier node = () -> "node-1";
    S3UploadService svc = new S3UploadService(cfg, s3, node);
    svc.uploadHour(hour);

    verify(s3, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void skips_whenFilesMissing_orHourMismatch() throws Exception {
    var cfg = new ModifiableCfg("temp-bucket");
    long hour = (System.currentTimeMillis() / cfg.getIdxExpiryDuration()) - 1;

    Path dir = tempDir.resolve("t").resolve("s").resolve(Long.toString(hour));
    Files.createDirectories(dir);
    // Only idx file present
    Files.writeString(dir.resolve("logfile.idx"), "IDX");

    cfg.setDataDir(tempDir.toString());
    cfg.setS3Bucket("bkt");
    cfg.setS3BasePrefix("logs");

    S3Client s3 = mock(S3Client.class);
    NodeIdSupplier node = () -> "node-1";
    S3UploadService svc = new S3UploadService(cfg, s3, node);
    svc.uploadHour(hour);
    verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));

    // different hour directory present should be ignored
    Path other = tempDir.resolve("t").resolve("s").resolve(Long.toString(hour + 1));
    Files.createDirectories(other);
    Files.writeString(other.resolve("logfile.idx"), "IDX");
    Files.writeString(other.resolve("logfile.bin"), "BIN");
    svc.uploadHour(hour);
    verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }
}
