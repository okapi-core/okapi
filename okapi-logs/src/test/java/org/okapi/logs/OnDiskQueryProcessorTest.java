package org.okapi.logs;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.logs.config.LogsConfigProperties;
import org.okapi.logs.io.LogFileWriter;
import org.okapi.logs.io.LogPage;
import org.okapi.logs.query.LevelFilter;
import org.okapi.logs.query.OnDiskQueryProcessor;
import org.okapi.logs.query.RegexFilter;
import org.okapi.logs.query.TraceFilter;
import org.okapi.logs.stats.NoOpStatsEmitter;
import org.okapi.protos.logs.LogPayloadProto;

class OnDiskQueryProcessorTest {
  private LogsConfigProperties cfg;
  private Path dataDir;

  @BeforeEach
  void setup() throws Exception {
    dataDir = Path.of("target/test-logs-odp");
    Files.createDirectories(dataDir);
    cfg = new LogsConfigProperties();
    cfg.setDataDir(dataDir.toString());
    cfg.setFsyncOnPageAppend(true);
  }

  @Test
  void query_byLevelTraceRegex_optimized() throws Exception {
    String tenant = "t1";
    String stream = "s1";
    LogPage page = TestCorpus.buildTestPage();
    LogFileWriter writer = new LogFileWriter(cfg);
    writer.appendPage(tenant, stream, page);

    OnDiskQueryProcessor qp = new OnDiskQueryProcessor(cfg, new NoOpStatsEmitter());

    long start = page.getTsStart() - 1000;
    long end = page.getTsEnd() + 1000;

    List<LogPayloadProto> warn = qp.getLogs(tenant, stream, start, end, new LevelFilter(30));
    assertEquals(2, warn.size());

    List<LogPayloadProto> tA =
        qp.getLogs(
            tenant,
            stream,
            start,
            end,
            new TraceFilter("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
    assertEquals(5, tA.size());

    List<LogPayloadProto> failed =
        qp.getLogs(tenant, stream, start, end, new RegexFilter("failed"));
    assertEquals(2, failed.size());
  }
}

