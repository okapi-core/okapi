package org.okapi.traces.upload;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

@Slf4j
public class TracefileMultipartUploader {

  private final S3Client s3;

  public TracefileMultipartUploader(S3Client s3) { this.s3 = s3; }

  public void upload(Path file, String bucket, String key) throws IOException {
    String uploadId = null;
    List<CompletedPart> completed = new ArrayList<>();
    try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
      var init = s3.createMultipartUpload(CreateMultipartUploadRequest.builder().bucket(bucket).key(key).build());
      uploadId = init.uploadId();

      long offset = 0L; int partNumber = 1;
      while (true) {
        if (offset + 8 > raf.length()) break;
        raf.seek(offset);
        int totalLen = raf.readInt(); // big-endian by DataInputStream; RAF.readInt reads big endian
        int crc32 = raf.readInt();
        long partLen = 8L + (long) totalLen;
        if (offset + partLen > raf.length()) break; // truncated

        byte[] buf = new byte[(int) partLen - 8];
        raf.readFully(buf);
        // buf is the inner page payload (without the 8 bytes header) due to earlier read

        var upr = UploadPartRequest.builder()
            .bucket(bucket)
            .key(key)
            .uploadId(uploadId)
            .partNumber(partNumber)
            .contentLength((long) buf.length + 8L) // include header size for clarity though we send only payload
            .build();
        // send header+payload together; reconstruct header bytes
        byte[] header = new byte[8];
        header[0] = (byte) ((totalLen >>> 24) & 0xFF);
        header[1] = (byte) ((totalLen >>> 16) & 0xFF);
        header[2] = (byte) ((totalLen >>> 8) & 0xFF);
        header[3] = (byte) (totalLen & 0xFF);
        header[4] = (byte) ((crc32 >>> 24) & 0xFF);
        header[5] = (byte) ((crc32 >>> 16) & 0xFF);
        header[6] = (byte) ((crc32 >>> 8) & 0xFF);
        header[7] = (byte) (crc32 & 0xFF);
        byte[] partBytes = new byte[header.length + buf.length];
        System.arraycopy(header, 0, partBytes, 0, header.length);
        System.arraycopy(buf, 0, partBytes, header.length, buf.length);

        var up = s3.uploadPart(upr, RequestBody.fromBytes(partBytes));
        completed.add(CompletedPart.builder().partNumber(partNumber).eTag(up.eTag()).build());
        partNumber++;
        offset += partLen;
      }

      var comp = CompleteMultipartUploadRequest.builder()
          .bucket(bucket)
          .key(key)
          .uploadId(uploadId)
          .multipartUpload(CompletedMultipartUpload.builder().parts(completed).build())
          .build();
      s3.completeMultipartUpload(comp);
    } catch (Exception e) {
      log.warn("Multipart upload failed for s3://{}/{}", bucket, key, e);
      if (uploadId != null) {
        try {
          s3.abortMultipartUpload(AbortMultipartUploadRequest.builder().bucket(bucket).key(key).uploadId(uploadId).build());
        } catch (Exception ignore) {}
      }
      if (e instanceof IOException io) throw io; else throw new IOException(e);
    }
  }
}

