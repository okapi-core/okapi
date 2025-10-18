package org.okapi.traces.spring;

import java.nio.file.Path;
import org.okapi.s3.ByteRangeCache;
import org.okapi.s3.SimpleByteRangeCache;
import org.okapi.traces.NodeIdSupplier;
import org.okapi.traces.metrics.NoopMetricsEmitter;
import org.okapi.traces.page.*;
import org.okapi.traces.query.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class TracesIoConfig {

  @Bean
  public TraceWriterConfig traceWriterConfig(
      @Value("${okapi.traces.writer.idleCloseMillis:60000}") long idleCloseMillis,
      @Value("${okapi.traces.writer.reapIntervalMillis:15000}") long reapIntervalMillis) {
    return TraceWriterConfig.builder()
        .idleCloseMillis(idleCloseMillis)
        .reapIntervalMillis(reapIntervalMillis)
        .build();
  }

  @Bean
  public TraceFileWriter traceFileWriter(
      @Value("${okapi.traces.baseDir:traces}") String baseDir, TraceWriterConfig config) {
    return new TraceFileWriter(Path.of(baseDir), config, new NoopMetricsEmitter());
  }

  @Bean
  public FlushStrategy flushStrategy(
      @Value("${okapi.traces.page.maxEstimatedBytes:8388608}") long maxEstimatedBytes) {
    return new SizeBasedFlushStrategy(maxEstimatedBytes); // default 8MB
  }

  @Bean
  public TraceBufferPoolManager bufferPoolManager(
      FlushStrategy strategy,
      TraceFileWriter writer,
      @Value("${okapi.traces.bloom.expectedInsertions:100000}") int expectedInsertions,
      @Value("${okapi.traces.bloom.fpp:0.01}") double fpp) {
    return new TraceBufferPoolManager(
        strategy, writer, new LogAndDropWriteFailedListener(), expectedInsertions, fpp);
  }

  // Query processors
  @Bean
  public TraceFileQueryProcessor fileTraceQueryProcessor(
      @Value("${okapi.traces.baseDir:traces}") String baseDir,
      @Value("${okapi.traces.query.threads:0}") int threads) {
    TraceQueryConfig cfg =
        TraceQueryConfig.builder()
            .queryThreads(
                threads > 0 ? threads : Math.max(1, Runtime.getRuntime().availableProcessors()))
            .build();
    return new TraceFileQueryProcessor(Path.of(baseDir), cfg);
  }

  @Bean
  public InMemoryTraceQueryProcessor inMemoryTraceQueryProcessor(
      TraceBufferPoolManager traceBufferPoolManager,
      @Value("${okapi.traces.query.threads:0}") int threads) {
    TraceQueryConfig cfg =
        TraceQueryConfig.builder()
            .queryThreads(
                threads > 0 ? threads : Math.max(1, Runtime.getRuntime().availableProcessors()))
            .build();
    return new InMemoryTraceQueryProcessor(traceBufferPoolManager, cfg, new NoopMetricsEmitter());
  }


  @Bean
  public ByteRangeCache s3ByteRangeCache(
      S3Client s3Client,
      @Value("${okapi.traces.s3.cache.maxPages:128}") int maxPages,
      @Value("${okapi.traces.s3.cache.expirySec:60}") long expirySec) {
    return new SimpleByteRangeCache(
        (bucket, key, start, endExclusive) ->
            s3Client
                .getObjectAsBytes(
                    software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .range("bytes=" + start + "-" + (endExclusive - 1))
                        .build())
                .asByteArray(),
        maxPages,
        16 * 1024,
        java.time.Duration.ofSeconds(expirySec));
  }

  @Bean
  public S3TracefileKeyResolver s3KeyResolver(
      @Value("${okapi.traces.s3.bucket:}") String bucket,
      @Value("${okapi.traces.s3.basePrefix:okapi}") String basePrefix) {
    return new SimpleS3TracefileKeyResolver(bucket, basePrefix);
  }

  @Bean
  public S3TraceQueryProcessor s3QueryProcessor(
      ByteRangeCache cache,
      S3TracefileKeyResolver resolver,
      S3Client s3Client,
      @Value("${okapi.traces.query.threads:0}") int threads) {
    TraceQueryConfig cfg =
        TraceQueryConfig.builder()
            .queryThreads(
                threads > 0 ? threads : Math.max(1, Runtime.getRuntime().availableProcessors()))
            .build();
    S3TraceQueryProcessor.ObjectLister lister =
        (bucket, prefix) -> {
          var req =
              software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
                  .bucket(bucket)
                  .prefix(prefix)
                  .build();
          var resp = s3Client.listObjectsV2(req);
          java.util.List<String> keys = new java.util.ArrayList<>();
          for (var o : resp.contents()) keys.add(o.key());
          return keys;
        };
    return new S3TraceQueryProcessor(cache, resolver, lister, cfg, new NoopMetricsEmitter());
  }

  @Bean
  @org.springframework.context.annotation.Primary
  public MultiplexingTraceQueryProcessor multiplexingTraceQueryProcessor(
      S3TraceQueryProcessor s3, InMemoryTraceQueryProcessor mem, TraceFileQueryProcessor file) {
    return new MultiplexingTraceQueryProcessor(java.util.List.of(s3, mem, file));
  }

  @Bean
  public NodeIdSupplier nodeIdSupplier(@Value("${okapi.traces.nodeId:}") String nodeIdOpt) {
    String envHost = System.getenv("HOSTNAME");
    String id =
        (nodeIdOpt != null && !nodeIdOpt.isBlank())
            ? nodeIdOpt
            : (envHost != null && !envHost.isBlank())
                ? envHost
                : java.util.UUID.randomUUID().toString();
    final String fixed = id;
    return () -> fixed;
  }
}
