package org.okapi.logs;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.logs.config.ModifiableCfg;
import org.okapi.logs.io.LogFileWriter;
import org.okapi.logs.query.LevelFilter;
import org.okapi.logs.query.OnDiskQueryProcessor;
import org.okapi.logs.query.RegexFilter;
import org.okapi.logs.query.TraceFilter;
import org.okapi.logs.stats.NoOpStatsEmitter;
import org.okapi.protos.logs.LogPayloadProto;

class OnDiskQueryProcessorTest {
  private ModifiableCfg cfg;
  @TempDir private Path dataDir;

  @BeforeEach
  void setup() throws Exception {
    Files.createDirectories(dataDir);
    cfg = new ModifiableCfg("test-bucket");
    cfg.setDataDir(dataDir.toString());
  }

  @Test
  void query_byLevelTraceRegex_optimized() throws Exception {
    var tenant = "t1";
    var stream = "s1";
    var page = TestCorpus.buildTestPage();
    var writer = new LogFileWriter(cfg);
    writer.appendPage(tenant, stream, page);

    var qp = new OnDiskQueryProcessor(cfg, new NoOpStatsEmitter());

    long start = page.getTsStart() - 1000;
    long end = page.getTsEnd() + 1000;

    List<LogPayloadProto> warn =
        qp.getLogs(
            tenant,
            stream,
            start,
            end,
            new LevelFilter(30),
            org.okapi.logs.query.QueryConfig.defaultConfig());
    assertEquals(2, warn.size());

    List<LogPayloadProto> tA =
        qp.getLogs(
            tenant,
            stream,
            start,
            end,
            new TraceFilter("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
            org.okapi.logs.query.QueryConfig.defaultConfig());
    assertEquals(5, tA.size());

    List<LogPayloadProto> failed =
        qp.getLogs(
            tenant,
            stream,
            start,
            end,
            new RegexFilter("failed"),
            org.okapi.logs.query.QueryConfig.defaultConfig());
    assertEquals(2, failed.size());
  }
}
