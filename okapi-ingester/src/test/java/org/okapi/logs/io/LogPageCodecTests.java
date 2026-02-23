/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.okapi.abstractio.TrigramUtil;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;

@Slf4j
public class LogPageCodecTests {

  @Test
  void testDecodingSingleEntryPage()
      throws IOException, StreamReadingException, NotEnoughBytesException {
    var logPage =
        LogPage.builder().expectedInsertions(100).maxSizeBytes(1000).maxRangeMs(2000L).build();
    var now = Instant.now().toEpochMilli();
    var record = new LogIngestRecord(now, "trace-a", 10, "log body");
    logPage.append(record);

    var codec = new LogPageNonChecksummedCodec();
    var serialized = codec.serialize(logPage);

    var maybeDeserialized = codec.deserialize(serialized);
    assertTrue(maybeDeserialized.isPresent());
    var deserialized = maybeDeserialized.get();
    assertTrue(deserialized.maybeContainsLeveInPage(10));
    assertFalse(deserialized.maybeContainsLeveInPage(20));
    assertTrue(deserialized.maybeContainsTraceId("trace-a"));
    assertFalse(deserialized.maybeContainsTraceId("trace-b"));
    assertTrue(deserialized.maybeContainsTrigram(TrigramUtil.getTrigramIndex('l', 'o', 'g')));
    assertFalse(deserialized.maybeContainsTrigram(TrigramUtil.getTrigramIndex('l', 'o', 'l')));
  }

  @Test
  void testDecodingMultipleEntryPage()
      throws IOException, StreamReadingException, NotEnoughBytesException {
    var logPage =
        LogPage.builder().expectedInsertions(100).maxSizeBytes(1000).maxRangeMs(2000L).build();
    var now = Instant.now().toEpochMilli();
    var record1 = new LogIngestRecord(now, "trace-a", 10, "log body");
    var record2 = new LogIngestRecord(now, "trace-b", 20, "lol body");
    logPage.append(record1);
    logPage.append(record2);

    var codec = new LogPageNonChecksummedCodec();
    var serialized = codec.serialize(logPage);

    var maybeDeserialized = codec.deserialize(serialized);
    assertTrue(maybeDeserialized.isPresent());
    var deserialized = maybeDeserialized.get();
    // check levels set
    assertTrue(deserialized.maybeContainsLeveInPage(10));
    assertTrue(deserialized.maybeContainsLeveInPage(20));
    assertFalse(deserialized.maybeContainsLeveInPage(30));

    // check trace sets
    assertTrue(deserialized.maybeContainsTraceId("trace-a"));
    assertTrue(deserialized.maybeContainsTraceId("trace-b"));
    assertFalse(deserialized.maybeContainsTraceId("trace-c"));

    // check trigrams
    assertTrue(deserialized.maybeContainsTrigram(TrigramUtil.getTrigramIndex('l', 'o', 'g')));
    assertTrue(deserialized.maybeContainsTrigram(TrigramUtil.getTrigramIndex('l', 'o', 'l')));
    assertFalse(deserialized.maybeContainsTrigram(TrigramUtil.getTrigramIndex('l', 'o', 'r')));
  }

  @Test
  void testDecodingRepeatingPage()
      throws IOException, StreamReadingException, NotEnoughBytesException {
    var logPage =
        LogPage.builder().expectedInsertions(100).maxSizeBytes(1000).maxRangeMs(2000L).build();
    var now = Instant.now().toEpochMilli();
    var record1 = new LogIngestRecord(now, "trace-a", 10, "log body");
    logPage.append(record1);
    logPage.append(record1);

    var codec = new LogPageNonChecksummedCodec();
    var serialized = codec.serialize(logPage);

    var maybeDeserialized = codec.deserialize(serialized);
    assertTrue(maybeDeserialized.isPresent());
    var deserialized = maybeDeserialized.get();
    assertEquals(2, deserialized.getNDocs());
  }
}
