package org.okapi.logs.stats;

public class NoOpStatsEmitter implements StatsEmitter {
  @Override public void indexEntriesLoaded(int n) {}
  @Override public void pagesScanned(int n) {}
  @Override public void docsRead(int n) {}
  @Override public void bytesRead(long n) {}
  @Override public void cacheHit(String cacheName) {}
  @Override public void cacheMiss(String cacheName) {}
}

