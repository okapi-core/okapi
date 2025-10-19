package org.okapi.logs.stats;

import io.micrometer.core.instrument.MeterRegistry;

public class OtelStatsEmitter implements StatsEmitter {
  private final MeterRegistry meterRegistry;

  public OtelStatsEmitter(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void indexEntriesLoaded(int n) {
    meterRegistry.counter("on_disk_index_entries_loaded").increment(n);
  }

  @Override
  public void pagesScanned(int n) {
    meterRegistry.counter("on_disk_pages_scanned").increment(n);
  }

  @Override
  public void docsRead(int n) {
    meterRegistry.counter("on_disk_docs_read").increment(n);
  }

  @Override
  public void bytesRead(long n) {
    meterRegistry.counter("on_disk_bytes_read").increment(n);
  }

  @Override
  public void cacheHit(String cacheName) {
    meterRegistry.counter("on_disk_cache_hits", "cache", cacheName).increment();
  }

  @Override
  public void cacheMiss(String cacheName) {
    meterRegistry.counter("on_disk_cache_misses", "cache", cacheName).increment();
  }
}

