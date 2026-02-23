package org.okapi.logs.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.abstractio.*;
import org.okapi.byterange.DiskByteRangeSupplier;
import org.okapi.byterange.LengthPrefixPageAndMdIterator;
import org.okapi.byterange.RangeIterationException;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;
import org.okapi.pages.MockAppendPage;
import org.okapi.pages.MockPageCodec;
import org.okapi.pages.MockPageInput;
import org.okapi.wal.lsn.Lsn;
import org.okapi.waltester.WalResourcesTestFactory;

@Slf4j
public class LogFileWriterTests {

  @TempDir Path tempDir;

  WalResourcesPerStream<String> walResourcesPerStream;

  @BeforeEach
  void setup() throws IOException {
    walResourcesPerStream = WalResourcesTestFactory.createResources(tempDir, Set.of("0"));
  }

  @Test
  void testAppendPage_SinglePage()
      throws IOException, RangeIterationException, StreamReadingException, NotEnoughBytesException {
    var page = new MockAppendPage(1000, 10000);
    var now = 1000L;
    var lsi = LogStreamIdentifier.of("0");
    page.append(new MockPageInput(now, "content", 10));
    var paths =
        new ExpiryDurationPartitionedPaths<String>(
            tempDir.resolve("data/logs"), 3600000L, "logfile.bin");
    var writer = new LogFileWriter<>(new MockPageCodec(), paths, walResourcesPerStream);
    writer.appendPage(lsi, page);

    // read the page back from disk
    var fp = paths.getLogBinFilePath(lsi, now);
    var diskByteSupplier = new DiskByteRangeSupplier(fp);
    var diskIterator = new LengthPrefixPageAndMdIterator(diskByteSupplier);
    var pageCodec = new MockPageCodec();
    var md = pageCodec.deserializeMetadata(diskIterator.readMetadata());
    assertTrue(md.isPresent());
    assertEquals(1000L, md.get().metadata().getTsStart());
    var bodyBytes = diskIterator.readPageBody();
    var deserializedPage = pageCodec.deserializeBody(bodyBytes, 0, bodyBytes.length);
    assertTrue(deserializedPage.isPresent());
    var body = deserializedPage.get();
    assertEquals(1, body.getInputs().size());
    assertEquals("content", body.getInputs().getFirst().getContent());
  }

  @Test
  void testAppendPage_MultiplePages()
      throws IOException, RangeIterationException, StreamReadingException, NotEnoughBytesException {
    var page1 = new MockAppendPage(1000, 10000);
    var page2 = new MockAppendPage(1000, 10000);
    var ts1 = 1000L;
    var ts2 = 2000L;
    var lsi = LogStreamIdentifier.of("0");
    page1.append(new MockPageInput(ts1, "content1", 10));
    page2.append(new MockPageInput(ts2, "content2", 10));
    page2.append(new MockPageInput(ts2 + 1, "content3", 10));

    var paths =
        new ExpiryDurationPartitionedPaths<String>(
            tempDir.resolve("data/logs"), 3600000L, "logfile.bin");
    var writer = new LogFileWriter<>(new MockPageCodec(), paths, walResourcesPerStream);
    writer.appendPage(lsi, page1);
    writer.appendPage(lsi, page2);

    // read the pages back from disk
    var fp = paths.getLogBinFilePath(lsi, ts1);
    var diskByteSupplier = new DiskByteRangeSupplier(fp);
    var diskIterator = new LengthPrefixPageAndMdIterator(diskByteSupplier);
    var pageCodec = new MockPageCodec();

    // first page
    var md1 = pageCodec.deserializeMetadata(diskIterator.readMetadata());
    assertTrue(md1.isPresent());
    assertEquals(ts1, md1.get().metadata().getTsStart());
    var bodyBytes1 = diskIterator.readPageBody();
    var deserializedPage1 = pageCodec.deserializeBody(bodyBytes1, 0, bodyBytes1.length);
    assertTrue(deserializedPage1.isPresent());
    var body1 = deserializedPage1.get();
    assertEquals(1, body1.getInputs().size());
    assertEquals("content1", body1.getInputs().getFirst().getContent());

    diskIterator.forward();

    // second page
    var md2 = pageCodec.deserializeMetadata(diskIterator.readMetadata());
    assertTrue(md2.isPresent());
    assertEquals(ts2, md2.get().metadata().getTsStart());
    var bodyBytes2 = diskIterator.readPageBody();
    var deserializedPage2 = pageCodec.deserializeBody(bodyBytes2, 0, bodyBytes2.length);
    assertTrue(deserializedPage2.isPresent());
    var body2 = deserializedPage2.get();
    assertEquals(2, body2.getInputs().size());
    assertEquals("content2", body2.getInputs().get(0).getContent());
    assertEquals("content3", body2.getInputs().get(1).getContent());
  }

  @Test
  void checkWalCommit() throws IOException {
    var lsi = LogStreamIdentifier.of("0");
    var walManager = walResourcesPerStream.getWalManager(lsi.getStreamId());
    var initialCommit = walManager.getCommittedLsn();
    assertTrue(initialCommit.isPresent());
    assertEquals(0L, initialCommit.get().getLsn().getNumber());

    var now = 1000L;
    var page = new MockAppendPage(1000, 10000);
    page.append(new MockPageInput(now, "content", 10));
    page.updateLsn(Lsn.fromNumber(5L));

    var paths =
        new ExpiryDurationPartitionedPaths<String>(
            tempDir.resolve("data/logs"), 3600000L, "logfile.bin");
    var writer = new LogFileWriter<>(new MockPageCodec(), paths, walResourcesPerStream);
    writer.appendPage(lsi, page);

    var updatedCommit = walManager.getCommittedLsn();
    assertTrue(updatedCommit.isPresent());
    assertEquals(5L, updatedCommit.get().getLsn().getNumber());
  }
}
