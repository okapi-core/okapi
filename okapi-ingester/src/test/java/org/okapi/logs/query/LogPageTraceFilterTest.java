package org.okapi.logs.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.byterange.RangeIterationException;
import org.okapi.logs.io.LogPageMetadata;

public class LogPageTraceFilterTest {

  LogPageMetadata metadata;

  @BeforeEach
  void setup() {
    metadata = LogPageMetadata.createEmptyMetadata(1000);
    metadata.putTraceId("trace-a");
    metadata.putTraceId("trace-b");
  }

  @Test
  void testShouldRead() throws RangeIterationException {
    var traceFilter = new LogPageTraceFilter("trace-a");
    assertTrue(traceFilter.shouldReadPage(metadata));
  }

  @Test
  void testShouldSkip() throws RangeIterationException {
    var traceFilter = new LogPageTraceFilter("trace-c");
    assertFalse(traceFilter.shouldReadPage(metadata));
  }
}
