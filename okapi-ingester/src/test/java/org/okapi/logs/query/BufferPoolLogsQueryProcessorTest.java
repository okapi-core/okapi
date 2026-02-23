/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.query;

import static org.junit.jupiter.api.Assertions.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.abstractio.LogStreamIdentifier;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.logs.LogsBufferPool;
import org.okapi.logs.io.*;
import org.okapi.logs.paths.LogsDiskPaths;
import org.okapi.logs.query.processor.BufferPoolLogsQueryProcessor;
import org.okapi.spring.configs.properties.LogsCfgImpl;
import org.okapi.wal.lsn.Lsn;
import org.okapi.waltester.WalResourcesTestFactory;

class BufferPoolLogsQueryProcessorTest {
  private LogsBufferPool pool;
  private BufferPoolLogsQueryProcessor qp;
  @TempDir Path tempDir;

  WalResourcesPerStream<String> walResourcesPerStream;

  @BeforeEach
  void setup() throws IOException {
    var cfg = new LogsCfgImpl();
    cfg.setS3Bucket("test-bucket");
    cfg.setMaxPageBytes(1024 * 1024);
    cfg.setMaxPageWindowMs(60_000);
    cfg.setDataDir(tempDir.toString());
    walResourcesPerStream = WalResourcesTestFactory.singleStreamSetup(tempDir);
    pool =
        new LogsBufferPool(
            cfg,
            (id) -> LogPage.builder().expectedInsertions(10000).maxRangeMs(1000L).build(),
            new LogPageNonChecksummedCodec(),
            new LogsDiskPaths(Path.of(cfg.getDataDir()), cfg.getIdxExpiryDuration(), "logfile.bin"),
            new SimpleMeterRegistry(),
            walResourcesPerStream);
    qp = new BufferPoolLogsQueryProcessor(pool);
  }

  @Test
  void returnsEmpty_whenNoActivePage() {
    var out =
        qp.getLogs("s", 0, Long.MAX_VALUE, new RegexPageFilter(".*"), QueryConfig.localSources());
    assertTrue(out.isEmpty());
  }

  @Test
  void outOfRange_returnsEmpty() {
    long now = System.currentTimeMillis();
    pool.append(
        Lsn.fromNumber(100),
        new LogStreamIdentifier("s"),
        new LogIngestRecord(now, "a", 20, "msg"));
    // query a non-overlapping far-future window
    var out =
        qp.getLogs(
            "s",
            now + 3600_000,
            now + 3600_000 + 1000,
            new RegexPageFilter(".*"),
            QueryConfig.localSources());
    assertTrue(out.isEmpty());
  }

  @Test
  void overlap_withFilters() {
    long base = System.currentTimeMillis();
    // 3 docs: two INFO (9) and one WARN (13)
    pool.append(
        Lsn.fromNumber(100),
        new LogStreamIdentifier("s"),
        new LogIngestRecord(base, "aaa", 9, "hello world"));
    pool.append(
        Lsn.fromNumber(200),
        new LogStreamIdentifier("s"),
        new LogIngestRecord(base + 1, "bbb", 13, "warn: disk low"));
    pool.append(
        Lsn.fromNumber(300),
        new LogStreamIdentifier("s"),
        new LogIngestRecord(base + 2, "aaa", 9, "hello again"));

    var all =
        qp.getLogs(
            "s", base - 1000, base + 5000, new RegexPageFilter(".*"), QueryConfig.localSources());
    assertEquals(3, all.size());

    var warns =
        qp.getLogs(
            "s", base - 1000, base + 5000, new LevelPageFilter(13), QueryConfig.localSources());
    assertEquals(1, warns.size());

    var traceA =
        qp.getLogs(
            "s",
            base - 1000,
            base + 5000,
            new LogPageTraceFilter("aaa"),
            QueryConfig.localSources());
    assertEquals(2, traceA.size());

    var regex =
        qp.getLogs(
            "s", base - 1000, base + 5000, new RegexPageFilter("warn"), QueryConfig.localSources());
    assertEquals(1, regex.size());
  }
}
