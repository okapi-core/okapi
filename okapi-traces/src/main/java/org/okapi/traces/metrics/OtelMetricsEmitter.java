package org.okapi.traces.metrics;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

public class OtelMetricsEmitter implements MetricsEmitter {
  private final Meter meter;
  private final LongCounter pageRead;
  private final LongCounter pageTimeSkipped;
  private final LongCounter bloomChecked;
  private final LongCounter bloomHit;
  private final LongCounter bloomMiss;
  private final LongCounter pageParsed;
  private final LongCounter pageParseError;
  private final LongCounter spansMatched;
  private final LongCounter writeBytes;
  private final LongCounter writeFailures;

  public OtelMetricsEmitter() {
    this.meter = GlobalOpenTelemetry.getMeter("org.okapi.traces");
    this.pageRead = meter.counterBuilder("okapi.traces.page_read").build();
    this.pageTimeSkipped = meter.counterBuilder("okapi.traces.page_time_skipped").build();
    this.bloomChecked = meter.counterBuilder("okapi.traces.bloom_checked").build();
    this.bloomHit = meter.counterBuilder("okapi.traces.bloom_hit").build();
    this.bloomMiss = meter.counterBuilder("okapi.traces.bloom_miss").build();
    this.pageParsed = meter.counterBuilder("okapi.traces.page_parsed").build();
    this.pageParseError = meter.counterBuilder("okapi.traces.page_parse_error").build();
    this.spansMatched = meter.counterBuilder("okapi.traces.spans_matched").build();
    this.writeBytes = meter.counterBuilder("okapi.traces.writer.bytes").build();
    this.writeFailures = meter.counterBuilder("okapi.traces.writer.failures").build();
  }

  @Override
  public void emitPageRead(String tenantId, String application) {
    pageRead.add(1);
  }

  @Override
  public void emitPageTimeSkipped(String tenantId, String application) {
    pageTimeSkipped.add(1);
  }

  @Override
  public void emitBloomChecked(String tenantId, String application) {
    bloomChecked.add(1);
  }

  @Override
  public void emitBloomHit(String tenantId, String application) {
    bloomHit.add(1);
  }

  @Override
  public void emitBloomMiss(String tenantId, String application) {
    bloomMiss.add(1);
  }

  @Override
  public void emitPageParsed(String tenantId, String application) {
    pageParsed.add(1);
  }

  @Override
  public void emitPageParseError(String tenantId, String application) {
    pageParseError.add(1);
  }

  @Override
  public void emitSpansMatched(String tenantId, String application, long count) {
    spansMatched.add(count);
  }

  @Override
  public void emitTracefileWriteBytes(
      String tenantId, String application, long hourBucket, long bytes) {
    writeBytes.add(bytes);
  }

  @Override
  public void emitTracefileWriteFailure(String tenantId, String application, long hourBucket) {
    writeFailures.add(1);
  }
}
