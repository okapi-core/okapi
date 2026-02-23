/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.abstractio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.pages.AppendOnlyPage;
import org.okapi.pages.Codec;
import org.okapi.streams.StreamIdentifier;

@Slf4j
@AllArgsConstructor
/// todo: rename this to DiskPageWriter (pages could we written out to other persistent mediums as
// well)
public class LogFileWriter<P extends AppendOnlyPage<?, S, M, B>, S, M, B, Id>
    implements PageWriter<P, S, M, B, Id> {
  private final Codec<P, S, M, B> codec;
  DiskLogBinPaths<Id> diskLogBinPaths;
  WalResourcesPerStream<Id> resourcesPerStream;

  public synchronized int appendPage(StreamIdentifier<Id> streamIdentifier, P page)
      throws IOException {
    byte[] bytes = codec.serialize(page);
    if (page.range().isEmpty() || page.isEmpty()) {
      return 0;
    }
    var path =
        diskLogBinPaths.getLogBinFilePath(streamIdentifier, page.range().get().startInclusive());
    Files.createDirectories(path.getParent());
    try (var ch =
        FileChannel.open(
            path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
      var written = ch.write(ByteBuffer.wrap(bytes));
      var lsn = page.getMaxLsn();
      var walManager = resourcesPerStream.getWalManager(streamIdentifier.getStreamId());
      walManager.commitLsn(lsn);
      return written;
    }
  }
}
