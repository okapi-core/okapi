package org.okapi.traces.spring;

import java.nio.file.Path;
import org.okapi.s3.S3ByteRangeCache;
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
  public BufferPoolManager bufferPoolManager(
      FlushStrategy strategy,
      TraceFileWriter writer,
      @Value("${okapi.traces.bloom.expectedInsertions:100000}") int expectedInsertions,
      @Value("${okapi.traces.bloom.fpp:0.01}") double fpp) {
    return new BufferPoolManager(
        strategy, writer, new LogAndDropWriteFailedListener(), expectedInsertions, fpp);
  }

  // Query processors
  @Bean
  public FileTraceQueryProcessor fileTraceQueryProcessor(
      @Value("${okapi.traces.baseDir:traces}") String baseDir,
      @Value("${okapi.traces.query.threads:0}") int threads) {
    TraceQueryConfig cfg =
        TraceQueryConfig.builder()
            .queryThreads(
                threads > 0 ? threads : Math.max(1, Runtime.getRuntime().availableProcessors()))
            .build();
    return new FileTraceQueryProcessor(Path.of(baseDir), cfg);
  }

  @Bean
  public InMemoryTraceQueryProcessor inMemoryTraceQueryProcessor(
      BufferPoolManager bufferPoolManager, @Value("${okapi.traces.query.threads:0}") int threads) {
    TraceQueryConfig cfg =
        TraceQueryConfig.builder()
            .queryThreads(
                threads > 0 ? threads : Math.max(1, Runtime.getRuntime().availableProcessors()))
            .build();
    return new InMemoryTraceQueryProcessor(bufferPoolManager, cfg, new NoopMetricsEmitter());
  }

  @Bean
  public S3Client s3Client() {
    return S3Client.create();
  }

  @Bean
  public S3ByteRangeCache s3ByteRangeCache(
      S3Client s3Client,
      @Value("${okapi.traces.s3.cache.maxPages:128}") int maxPages,
      @Value("${okapi.traces.s3.cache.expirySec:60}") long expirySec) {
    return new S3ByteRangeCache(s3Client, maxPages, java.time.Duration.ofSeconds(expirySec));
  }

  @Bean
  public S3TracefileKeyResolver s3KeyResolver(
      @Value("${okapi.traces.s3.bucket:}") String bucket,
      @Value("${okapi.traces.s3.basePrefix:okapi}") String basePrefix) {
    return new SimpleS3TracefileKeyResolver(bucket, basePrefix);
  }

  @Bean
  public S3QueryProcessor s3QueryProcessor(
      S3ByteRangeCache cache,
      S3TracefileKeyResolver resolver,
      @Value("${okapi.traces.query.threads:0}") int threads) {
    TraceQueryConfig cfg =
        TraceQueryConfig.builder()
            .queryThreads(
                threads > 0 ? threads : Math.max(1, Runtime.getRuntime().availableProcessors()))
            .build();
    return new S3QueryProcessor(cache, resolver, cfg, new NoopMetricsEmitter());
  }

  @Bean
  @org.springframework.context.annotation.Primary
  public MultiplexingTraceQueryProcessor multiplexingTraceQueryProcessor(
      S3QueryProcessor s3, InMemoryTraceQueryProcessor mem, FileTraceQueryProcessor file) {
    return new MultiplexingTraceQueryProcessor(java.util.List.of(s3, mem, file));
  }
}
