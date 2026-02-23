package org.okapi.testmodules;

import com.google.inject.Inject;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.okapi.abstractio.DiskLogBinPaths;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.pages.Codec;
import org.okapi.streams.StreamIdentifier;
import org.okapi.traces.TracesBufferPool;
import org.okapi.traces.config.TracesCfg;
import org.okapi.traces.io.SpanIngestionRecord;
import org.okapi.traces.io.SpanPage;
import org.okapi.traces.io.SpanPageBody;
import org.okapi.traces.io.SpanPageCodec;
import org.okapi.traces.io.SpanPageMetadata;
import org.okapi.traces.io.SpanPageSnapshot;
import org.okapi.traces.paths.TracesDiskPaths;
import org.okapi.wal.lsn.Lsn;

public class LoggingTracesBufferPool extends TracesBufferPool {
  public record ConsumeCommand(Lsn lsn, String streamId, SpanIngestionRecord record) {}

  @Getter private final List<ConsumeCommand> commands = new ArrayList<>();

  @Inject
  public LoggingTracesBufferPool(
      TracesCfg cfg,
      DiskLogBinPaths<String> diskLogBinPaths,
      MeterRegistry meterRegistry,
      WalResourcesPerStream<String> walResourcesPerStream) {
    super(
        cfg,
        streamId ->
            new SpanPage(
                cfg.getExpectedInsertions(),
                cfg.getBloomFpp(),
                cfg.getMaxPageWindowMs(),
                cfg.getMaxPageBytes()),
        (Codec<SpanPage, SpanPageSnapshot, SpanPageMetadata, SpanPageBody>) new SpanPageCodec(),
        (TracesDiskPaths) diskLogBinPaths,
        meterRegistry,
        walResourcesPerStream);
  }

  @Override
  public void consume(
      Lsn lsn, StreamIdentifier<String> streamIdentifier, SpanIngestionRecord record) {
    commands.add(new ConsumeCommand(lsn, streamIdentifier.getStreamId(), record));
    super.consume(lsn, streamIdentifier, record);
  }
}
