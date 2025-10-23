package org.okapi.logs.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
  private final MemberSetQueryProcessor memberSet;
  private final ExecutorService exec = Executors.newFixedThreadPool(4);

  public MultiSourceQueryProcessor(
      BufferPoolQueryProcessor buffer,
      OnDiskQueryProcessor disk,
      S3QueryProcessor s3,
      MemberSetQueryProcessor memberSet) {
    this.buffer = buffer;
    this.disk = disk;
    this.s3 = s3;
    this.memberSet = memberSet;
  }

  @Override
  public List<LogPayloadProto> getLogs(
      String tenantId, String logStream, long start, long end, LogFilter filter, QueryConfig cfg)
      throws IOException {
    QueryConfig effective = (cfg == null) ? QueryConfig.defaultConfig() : cfg;

    CompletableFuture<List<LogPayloadProto>> fromBuf =
        effective.bufferPool
            ? CompletableFuture.supplyAsync(
                () -> buffer.getLogs(tenantId, logStream, start, end, filter, effective), exec)
            : CompletableFuture.completedFuture(List.of());
    CompletableFuture<List<LogPayloadProto>> fromDisk =
        effective.disk
            ? CompletableFuture.supplyAsync(
                () -> {
                  try {
                    return disk.getLogs(tenantId, logStream, start, end, filter, effective);
                  } catch (IOException e) {
                    throw new CompletionException(e);
                  }
                },
                exec)
            : CompletableFuture.completedFuture(List.of());
    CompletableFuture<List<LogPayloadProto>> fromS3 =
        (effective.s3 && !effective.fanOut)
            ? CompletableFuture.supplyAsync(
                () -> {
                  try {
                    return s3.getLogs(tenantId, logStream, start, end, filter, effective);
                  } catch (IOException e) {
                    throw new CompletionException(e);
                  }
                },
                exec)
            : CompletableFuture.completedFuture(List.of());
    CompletableFuture<List<LogPayloadProto>> fromMember =
        (!effective.fanOut)
            ? CompletableFuture.supplyAsync(
                () -> {
                  try {
                    return memberSet.getLogs(tenantId, logStream, start, end, filter, effective);
                  } catch (IOException e) {
                    throw new CompletionException(e);
                  }
                },
                exec)
            : CompletableFuture.completedFuture(List.of());

    List<LogPayloadProto> out = new ArrayList<>();
    try {
      out.addAll(fromBuf.join());
      out.addAll(fromDisk.join());
      out.addAll(fromS3.join());
      out.addAll(fromMember.join());
    } catch (RuntimeException re) {
      if (re.getCause() instanceof IOException ioe) throw ioe;
      throw re;
    }
    return out;
  }
}
