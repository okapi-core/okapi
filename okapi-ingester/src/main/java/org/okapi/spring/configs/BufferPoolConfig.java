package org.okapi.spring.configs;

import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Path;
import org.okapi.abstractbufferpool.BUFFER_POOL_TYPE;
import org.okapi.abstractio.PartNames;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.logs.LogsBufferPool;
import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.io.LogPage;
import org.okapi.logs.io.LogPageNonChecksummedCodec;
import org.okapi.logs.paths.LogsDiskPaths;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.metrics.io.MetricsPage;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.metrics.paths.MetricsDiskPaths;
import org.okapi.pages.MetricsBufferPool;
import org.okapi.traces.TracesBufferPool;
import org.okapi.traces.config.TracesCfg;
import org.okapi.traces.io.SpanPage;
import org.okapi.traces.io.SpanPageCodec;
import org.okapi.traces.paths.TracesDiskPaths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class BufferPoolConfig {

  @Bean
  public LogsDiskPaths diskBinPathsLogs(@Autowired LogsCfg cfg) {
    return new LogsDiskPaths(
        Path.of(cfg.getDataDir()), cfg.getIdxExpiryDuration(), PartNames.LOG_FILE_PART);
  }

  @Bean
  public TracesDiskPaths diskBinPathsTraces(@Autowired TracesCfg cfg) {
    return new TracesDiskPaths(
        Path.of(cfg.getDataDir()), cfg.getIdxExpiryDuration(), PartNames.SPAN_FILE_PART);
  }

  @Bean
  public MetricsDiskPaths diskBinPathsMetrics(@Autowired MetricsCfg cfg) {
    return new MetricsDiskPaths(
        Path.of(cfg.getDataDir()), cfg.getIdxExpiryDuration(), PartNames.METRICS_FILE_PART);
  }

  @Bean
  public LogsBufferPool bufferPool(
      @Autowired LogsCfg cfg,
      @Autowired MeterRegistry meterRegistry,
      @Autowired LogsDiskPaths diskLogBinPaths,
      @Autowired WalResourcesPerStream<String> walResourcesPerStream) {
    return new LogsBufferPool(
        cfg,
        (id) ->
            LogPage.builder()
                .expectedInsertions(cfg.getExpectedInsertions())
                .maxRangeMs(cfg.getMaxPageWindowMs())
                .maxSizeBytes(cfg.getMaxPageBytes())
                .build(),
        new LogPageNonChecksummedCodec(),
        diskLogBinPaths,
        meterRegistry,
        walResourcesPerStream);
  }

  @Bean
  public TracesBufferPool spanBufferPool(
      @Autowired TracesCfg cfg,
      @Autowired MeterRegistry meterRegistry,
      @Autowired TracesDiskPaths diskLogBinPaths,
      @Autowired @Qualifier(Qualifiers.TRACES_WAL_RESOURCES)
          WalResourcesPerStream<String> walResourcesPerStream) {
    return new TracesBufferPool(
        cfg,
        (id) ->
            SpanPage.builder()
                .expectedInsertions(cfg.getExpectedInsertions())
                .maxRangeMs(cfg.getMaxPageWindowMs())
                .maxSizeBytes(cfg.getMaxPageBytes())
                .fpp(cfg.getBloomFpp())
                .build(),
        new SpanPageCodec(),
        diskLogBinPaths,
        meterRegistry,
        walResourcesPerStream);
  }

  @Bean
  public MetricsBufferPool metricsBufferPool(
      @Autowired MetricsCfg cfg,
      @Autowired MeterRegistry meterRegistry,
      @Autowired MetricsDiskPaths diskLogBinPaths,
      @Autowired @Qualifier(Qualifiers.METRICS_WAL_RESOURCES)
          WalResourcesPerStream<String> walResourcesPerStream) {
    return new MetricsBufferPool(
        BUFFER_POOL_TYPE.METRICS,
        (id) ->
            new MetricsPage(
                cfg.getMaxPageWindowMs(),
                cfg.getMaxPageBytes(),
                cfg.getExpectedInsertions(),
                cfg.getBloomFpp()),
        new MetricsPageCodec(),
        diskLogBinPaths,
        meterRegistry,
        cfg.getSealedPageCap(),
        cfg.getSealedPageTtlMs(),
        walResourcesPerStream);
  }
}
