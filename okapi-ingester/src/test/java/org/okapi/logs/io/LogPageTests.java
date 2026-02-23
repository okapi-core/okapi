package org.okapi.logs.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.okapi.abstractio.TrigramUtil;

@Slf4j
public class LogPageTests {

  public String randomString() {
    return RandomStringUtils.secure().next(200, true, false);
  }

  @Test
  void testWritingSinglePage() throws IOException {
    var page =
        LogPage.builder().maxSizeBytes(1000).maxRangeMs(200L).expectedInsertions(20000).build();
    var now = Instant.now().toEpochMilli();
    var record = new LogIngestRecord(now, "trace-a", 10, "log level 1");
    page.append(record);
    assertEquals(1, page.getNDocs());
    assertFalse(page.isFull());
  }

  @Test
  void testWritingMultiplePages() {

    var page =
        LogPage.builder()
            .maxSizeBytes(1000)
            .maxRangeMs(Duration.of(10, ChronoUnit.MINUTES).toMillis())
            .expectedInsertions(20000)
            .build();
    var start = Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli();
    var record1 = new LogIngestRecord(start, "trace-a", 10, "log level 1");
    var record2 =
        new LogIngestRecord(
            start + Duration.of(10, ChronoUnit.SECONDS).toMillis(), "trace-b", 20, "log level 2");
    page.append(record1);
    page.append(record2);
    assertEquals(2, page.getNDocs());
    assertFalse(page.isFull());
    assertTrue(page.maybeContainsLeveInPage(10));
    assertTrue(page.maybeContainsLeveInPage(20));
    assertFalse(page.maybeContainsLeveInPage(30));
    assertTrue(page.maybeContainsTraceId("trace-a"));
    assertTrue(page.maybeContainsTraceId("trace-b"));
    assertFalse(page.maybeContainsTraceId("trace-c"));

    var trigrams1 = TrigramUtil.extractAsciiTrigramIndices("log level 1");
    for (var tri : trigrams1) {
      assertTrue(page.maybeContainsTrigram(tri));
    }
    var trigrams2 = TrigramUtil.extractAsciiTrigramIndices("log level 2");
    for (var tri : trigrams2) {
      assertTrue(page.maybeContainsTrigram(tri));
    }
    assertFalse(page.maybeContainsTrigram(TrigramUtil.getTrigramIndex('a', 'z', 'b')));
  }

  @Test
  void testWritingBeyondTimeCapacity() {
    var page =
        LogPage.builder()
            .maxSizeBytes(1000)
            .maxRangeMs(Duration.of(1, ChronoUnit.SECONDS).toMillis())
            .expectedInsertions(20000)
            .build();
    var start = Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli();
    var record1 = new LogIngestRecord(start, "trace-a", 10, "log level 1");
    var record2 =
        new LogIngestRecord(
            start + Duration.of(10, ChronoUnit.SECONDS).toMillis(), "trace-b", 20, "log level 2");
    page.append(record1);
    page.append(record2);
    assertTrue(page.isFull());
  }

  @Test
  void testWritingBeyondSizeCapacity() {
    var page =
        LogPage.builder()
            .maxSizeBytes(10)
            .maxRangeMs(Duration.of(1, ChronoUnit.DAYS).toMillis())
            .expectedInsertions(20000)
            .build();
    var start = Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli();
    var record1 = new LogIngestRecord(start, "trace-a", 10, "log level 1");
    var record2 =
        new LogIngestRecord(
            start + Duration.of(10, ChronoUnit.SECONDS).toMillis(), "trace-b", 20, "log level 2");
    page.append(record1);
    page.append(record2);
    assertTrue(page.isFull());
  }

  @Test
  void testPageWithoutAppendsIsMarkedEmpty() {
    var page =
        LogPage.builder()
            .maxSizeBytes(1000)
            .maxRangeMs(Duration.of(1, ChronoUnit.DAYS).toMillis())
            .expectedInsertions(20000)
            .build();
    assertFalse(page.isFull());
    assertTrue(page.isEmpty());
  }
}
