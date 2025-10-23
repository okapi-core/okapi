package org.okapi.logs.query;

import static org.junit.jupiter.api.Assertions.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.logs.config.LogsConfigProperties;
import org.okapi.logs.runtime.LogPageBufferPool;
import org.okapi.protos.logs.LogPayloadProto;

class BufferPoolQueryProcessorTest {
  private LogPageBufferPool pool;
  private BufferPoolQueryProcessor qp;

  @BeforeEach
  void setup() {
    LogsConfigProperties cfg = new LogsConfigProperties();
    cfg.setMaxDocsPerPage(100);
    cfg.setMaxPageBytes(1024 * 1024);
    cfg.setMaxPageWindowMs(60_000);
    pool = new LogPageBufferPool(cfg, new SimpleMeterRegistry());
    qp = new BufferPoolQueryProcessor(pool);
  }

  @Test
  void returnsEmpty_whenNoActivePage() {
    List<LogPayloadProto> out =
        qp.getLogs("t", "s", 0, Long.MAX_VALUE, new RegexFilter(".*"), QueryConfig.defaultConfig());
    assertTrue(out.isEmpty());
  }

  @Test
  void outOfRange_returnsEmpty() {
    long now = System.currentTimeMillis();
    pool.consume("t", "s", now, "a", 20, "msg");
    // query a non-overlapping far-future window
    List<LogPayloadProto> out =
        qp.getLogs(
            "t",
            "s",
            now + 3600_000,
            now + 3600_000 + 1000,
            new RegexFilter(".*"),
            QueryConfig.defaultConfig());
    assertTrue(out.isEmpty());
  }

  @Test
  void overlap_withFilters() {
    long base = System.currentTimeMillis();
    // 3 docs: two INFO (9) and one WARN (13)
    pool.consume("t", "s", base, "aaa", 9, "hello world");
    pool.consume("t", "s", base + 1, "bbb", 13, "warn: disk low");
    pool.consume("t", "s", base + 2, "aaa", 9, "hello again");

    List<LogPayloadProto> all =
        qp.getLogs(
            "t", "s", base - 1000, base + 5000, new RegexFilter(".*"), QueryConfig.defaultConfig());
    assertEquals(3, all.size());

    List<LogPayloadProto> warns =
        qp.getLogs("t", "s", base - 1000, base + 5000, new LevelFilter(13), QueryConfig.defaultConfig());
    assertEquals(1, warns.size());

    List<LogPayloadProto> traceA =
        qp.getLogs(
            "t", "s", base - 1000, base + 5000, new TraceFilter("aaa"), QueryConfig.defaultConfig());
    assertEquals(2, traceA.size());

    List<LogPayloadProto> regex =
        qp.getLogs(
            "t", "s", base - 1000, base + 5000, new RegexFilter("warn"), QueryConfig.defaultConfig());
    assertEquals(1, regex.size());
  }
}
