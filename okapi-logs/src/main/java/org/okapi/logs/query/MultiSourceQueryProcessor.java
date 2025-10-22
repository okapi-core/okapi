package org.okapi.logs.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.okapi.protos.logs.LogPayloadProto;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class MultiSourceQueryProcessor implements QueryProcessor {
  private final BufferPoolQueryProcessor buffer;
  private final OnDiskQueryProcessor disk;
  private final S3QueryProcessor s3;
  private final ExecutorService exec = Executors.newFixedThreadPool(3);

  public MultiSourceQueryProcessor(
      BufferPoolQueryProcessor buffer, OnDiskQueryProcessor disk, S3QueryProcessor s3) {
    this.buffer = buffer;
    this.disk = disk;
    this.s3 = s3;
  }

  @Override
  public List<LogPayloadProto> getLogs(
      String tenantId, String logStream, long start, long end, LogFilter filter)
      throws IOException {
    CompletableFuture<List<LogPayloadProto>> fromBuf =
        CompletableFuture.supplyAsync(
            () -> buffer.getLogs(tenantId, logStream, start, end, filter), exec);
    CompletableFuture<List<LogPayloadProto>> fromDisk =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                return disk.getLogs(tenantId, logStream, start, end, filter);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            },
            exec);
    CompletableFuture<List<LogPayloadProto>> fromS3 =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                return s3.getLogs(tenantId, logStream, start, end, filter);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            },
            exec);

    List<LogPayloadProto> out = new ArrayList<>();
    try {
      out.addAll(fromBuf.join());
      out.addAll(fromDisk.join());
      out.addAll(fromS3.join());
    } catch (RuntimeException re) {
      if (re.getCause() instanceof IOException ioe) throw ioe;
      throw re;
    }
    return out;
  }
}
