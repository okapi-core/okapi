package org.okapi.traces.upload;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.okapi.traces.page.SpanPage;
import java.util.List;
import org.okapi.traces.NodeIdSupplier;
import org.okapi.traces.query.S3TracefileKeyResolver;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

public class TracefileUploadTest {

  Path dir;

  @BeforeEach
  void setup() throws Exception {
    dir = Files.createTempDirectory("okapi-upload");
  }

  @AfterEach
  void cleanup() throws Exception {
    if (dir != null) {
      Files.walk(dir)
          .sorted((a,b)->b.getNameCount()-a.getNameCount())
          .forEach(p -> { try { Files.deleteIfExists(p);} catch (Exception ignored) {} });
    }
  }

  @Test
  void multipartUploader_success_and_abort_on_failure() throws Exception {
    // Build a single-page file
    var page = SpanPage.newEmpty(100, 0.01);
    long now = 1700000000000L;
    var req = io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest.newBuilder()
        .addResourceSpans(io.opentelemetry.proto.trace.v1.ResourceSpans.newBuilder()
            .addScopeSpans(io.opentelemetry.proto.trace.v1.ScopeSpans.newBuilder()
                .addSpans(io.opentelemetry.proto.trace.v1.Span.newBuilder()
                    .setTraceId(com.google.protobuf.ByteString.copyFrom(new byte[16]))
                    .setSpanId(com.google.protobuf.ByteString.copyFrom(new byte[8]))
                    .setStartTimeUnixNano(now*1_000_000L)
                    .setEndTimeUnixNano((now+1)*1_000_000L)
                    .build()))
            .build())
        .build();
    page.append(req);
    byte[] bytes = page.serialize();
    Path file = dir.resolve("tracefile.100.bin");
    Files.write(file, bytes);

    // Success path
    S3Client s3 = mock(S3Client.class);
    when(s3.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
        .thenReturn(CreateMultipartUploadResponse.builder().uploadId("u1").build());
    when(s3.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
        .thenReturn(UploadPartResponse.builder().eTag("etag1").build());
    doReturn(null).when(s3).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));

    new TracefileMultipartUploader(s3).upload(file, "bucket", "key");
    verify(s3, times(1)).createMultipartUpload(any(CreateMultipartUploadRequest.class));
    verify(s3, times(1)).uploadPart(any(UploadPartRequest.class), any(RequestBody.class));
    verify(s3, times(1)).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));

    // Failure path: uploadPart throws, triggers abort
    S3Client s3fail = mock(S3Client.class);
    when(s3fail.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
        .thenReturn(CreateMultipartUploadResponse.builder().uploadId("u2").build());
    when(s3fail.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
        .thenThrow(new RuntimeException("boom"));
    doReturn(null).when(s3fail).abortMultipartUpload(any(AbortMultipartUploadRequest.class));

    assertThrows(Exception.class, () -> new TracefileMultipartUploader(s3fail).upload(file, "b", "k"));
    verify(s3fail, times(1)).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
  }

  @Test
  void uploadJob_uploads_past_files_and_deletes() throws Exception {
    // Build a file older than current hour bucket under baseDir/tenant/app
    long nowBucket = System.currentTimeMillis() / 3_600_000L;
    String tenant = "t"; String app = "a";
    Path base = dir.resolve(tenant).resolve(app);
    Files.createDirectories(base);
    Path old = base.resolve("tracefile." + (nowBucket-1) + ".bin");
    Files.write(old, new byte[]{0,0,0,0, 0,0,0,0}); // minimal header for job scanning

    S3Client s3 = mock(S3Client.class);
    S3TracefileKeyResolver resolver = new S3TracefileKeyResolver() {
      public String bucket() { return "B"; }
      public List<String> keyFor(String t, String a, long hb) { return java.util.List.of(t+"/"+a+"/"+hb+"/"); }
      public String uploadKey(String t, String a, long hb, String nodeId) { return t+"/"+a+"/"+hb+"/"+nodeId+"/tracefile.bin"; }
    };
    NodeIdSupplier node = () -> "node1";
    TracefileUploadJob job = new TracefileUploadJob(s3, resolver, node);
    // Inject baseDir via reflection since field is @Value-injected normally
    var f = TracefileUploadJob.class.getDeclaredField("baseDir");
    f.setAccessible(true);
    f.set(job, dir);

    // Stub uploader path: we don't verify multipart details here; just that S3 methods are invoked through uploader
    // We'll mock S3 client methods to no-op in uploader
    when(s3.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
        .thenReturn(CreateMultipartUploadResponse.builder().uploadId("u1").build());
    when(s3.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
        .thenReturn(UploadPartResponse.builder().eTag("e").build());
    doReturn(null).when(s3).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));

    job.runUpload();

    assertFalse(Files.exists(old)); // deleted after upload
    verify(s3, atLeastOnce()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
    verify(s3, atLeastOnce()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
  }
}
