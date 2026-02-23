package org.okapi.pages;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractbufferpool.BUFFER_POOL_TYPE;
import org.okapi.abstractbufferpool.PeriodicallyFlushedBufPool;
import org.okapi.abstractio.DiskLogBinPaths;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.metrics.io.MetricsPage;
import org.okapi.metrics.io.MetricsPageBody;
import org.okapi.metrics.io.MetricsPageMetadata;
import org.okapi.metrics.io.MetricsPageSnapshot;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.streams.StreamIdentifier;
import org.okapi.wal.lsn.Lsn;

@Slf4j
public class MetricsBufferPool
    extends PeriodicallyFlushedBufPool<
        ExportMetricsRequest,
        MetricsPage,
        ExportMetricsRequest,
        MetricsPageSnapshot,
        MetricsPageMetadata,
        MetricsPageBody,
        String> {

  public MetricsBufferPool(
      BUFFER_POOL_TYPE poolType,
      Function<StreamIdentifier<String>, MetricsPage> pageFactory,
      Codec<MetricsPage, MetricsPageSnapshot, MetricsPageMetadata, MetricsPageBody> codec,
      DiskLogBinPaths<String> diskLogBinPaths,
      MeterRegistry meterRegistry,
      int sealedPageCap,
      long sealedPageTtlMs,
      WalResourcesPerStream<String> walResourcesPerStream) {
    super(
        poolType,
        pageFactory,
        codec,
        diskLogBinPaths,
        meterRegistry,
        sealedPageCap,
        sealedPageTtlMs,
        walResourcesPerStream);
  }

  @Override
  public void consume(Lsn lsn, StreamIdentifier<String> identifier, ExportMetricsRequest record) {
    append(lsn, identifier, record);
  }
}
