package org.okapi.testmodules;

import com.google.inject.Inject;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.okapi.abstractbufferpool.BUFFER_POOL_TYPE;
import org.okapi.abstractio.DiskLogBinPaths;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.metrics.io.MetricsPage;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.pages.MetricsBufferPool;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.streams.StreamIdentifier;
import org.okapi.wal.lsn.Lsn;

public class LoggingMetricsBufferPool extends MetricsBufferPool {
  public record ConsumeCommand(Lsn lsn, String streamId, ExportMetricsRequest request) {}

  @Getter private final List<ConsumeCommand> commands = new ArrayList<>();

  @Inject
  public LoggingMetricsBufferPool(
      MetricsCfg metricsCfg,
      DiskLogBinPaths<String> diskLogBinPaths,
      MeterRegistry meterRegistry,
      WalResourcesPerStream<String> walResourcesPerStream) {
    super(
        BUFFER_POOL_TYPE.METRICS,
        streamId ->
            new MetricsPage(
                metricsCfg.getMaxPageWindowMs(),
                metricsCfg.getMaxPageBytes(),
                metricsCfg.getExpectedInsertions(),
                metricsCfg.getBloomFpp()),
        new MetricsPageCodec(),
        diskLogBinPaths,
        meterRegistry,
        metricsCfg.getSealedPageCap(),
        metricsCfg.getSealedPageTtlMs(),
        walResourcesPerStream);
  }

  @Override
  public void consume(Lsn lsn, StreamIdentifier<String> identifier, ExportMetricsRequest record) {
    commands.add(new ConsumeCommand(lsn, identifier.getStreamId(), record));
    super.consume(lsn, identifier, record);
  }
}
