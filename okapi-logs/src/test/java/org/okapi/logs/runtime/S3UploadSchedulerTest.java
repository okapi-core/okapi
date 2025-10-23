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

class S3UploadSchedulerTest {

  @TempDir Path tempDir;

  @Test
  void uploads_oldHour_andSkips_currentHour_andRespectsMarker() throws Exception {
    var cfg = new ModifiableCfg("temp-bucket");
    long nowHour = System.currentTimeMillis() / cfg.getIdxExpiryDuration();
    long oldHour = nowHour - 1;

    Path oldDir = tempDir.resolve("t").resolve("s").resolve(Long.toString(oldHour));
    Files.createDirectories(oldDir);
    Files.writeString(oldDir.resolve("logfile.idx"), "IDX");
    Files.writeString(oldDir.resolve("logfile.bin"), "BIN");

    Path curDir = tempDir.resolve("t").resolve("s").resolve(Long.toString(nowHour));
    Files.createDirectories(curDir);
    Files.writeString(curDir.resolve("logfile.idx"), "IDX");
    Files.writeString(curDir.resolve("logfile.bin"), "BIN");

    cfg.setDataDir(tempDir.toString());
    cfg.setS3Bucket("bkt");
    cfg.setS3BasePrefix("logs");
    // Ensure current hour is not eligible by setting grace to one full hour
    cfg.setS3UploadGraceMs(3_600_000);

    S3Client s3 = mock(S3Client.class);
    NodeIdSupplier node = () -> "n1";
    S3UploadScheduler sched = new S3UploadScheduler(cfg, s3, node);

    // First tick should upload old hour only
    sched.onTick();
    verify(s3, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));

    // Second tick without changes should skip due to marker
    reset(s3);
    sched.onTick();
    verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));

    // Modify file to force re-upload
    Files.writeString(oldDir.resolve("logfile.idx"), "IDX2");
    sched.onTick();
    verify(s3, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }
}
