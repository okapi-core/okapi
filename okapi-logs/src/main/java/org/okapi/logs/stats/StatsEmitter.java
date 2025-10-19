package org.okapi.logs.stats;

public interface StatsEmitter {
  void indexEntriesLoaded(int n);
  void pagesScanned(int n);
  void docsRead(int n);
  void bytesRead(long n);
  void cacheHit(String cacheName);
  void cacheMiss(String cacheName);
}

