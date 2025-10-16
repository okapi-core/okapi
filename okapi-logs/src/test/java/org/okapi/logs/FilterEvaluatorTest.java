package org.okapi.logs;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.okapi.logs.io.LogPage;
import org.okapi.logs.query.LevelFilter;
import org.okapi.logs.query.FilterEvaluator;
import org.okapi.logs.query.RegexFilter;
import org.okapi.logs.query.TraceFilter;
import org.okapi.protos.logs.LogPayloadProto;

class FilterEvaluatorTest {
  @Test
  void level_trace_regex_filters() {
    LogPage page =
        LogPage.builder()
            .traceIdSet(BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 100))
            .expectedInsertions(100)
            .build();

    page.append(1L, "t1", 20, "hello world");
    page.append(2L, "t1", 40, "error occurred");
    page.append(3L, "t2", 30, "warning sign");

    List<LogPayloadProto> warn = FilterEvaluator.apply(page, new LevelFilter(30));
    assertEquals(1, warn.size());
    List<LogPayloadProto> t1 = FilterEvaluator.apply(page, new TraceFilter("t1"));
    assertEquals(2, t1.size());
    List<LogPayloadProto> regex = FilterEvaluator.apply(page, new RegexFilter("error"));
    assertEquals(1, regex.size());
  }
}

