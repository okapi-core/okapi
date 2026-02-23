package org.okapi.abstractio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;
import lombok.extern.slf4j.Slf4j;
import org.okapi.streams.StreamIdentifier;

@Slf4j
public class ExpiryDurationPartitionedPaths<Id> implements DiskLogBinPaths<Id> {
  Path dataDir;
  long idxExpiryDuration;
  String baseFileName;

  public ExpiryDurationPartitionedPaths(Path dataDir, long idxExpiryDuration, String baseFileName) {
    this.dataDir = dataDir;
    this.idxExpiryDuration = idxExpiryDuration;
    this.baseFileName = baseFileName;
  }

  @Override
  public Path getLogBinFilePath(StreamIdentifier<Id> streamIdentifier, long pageStartMillis) {
    return dataDir
        .resolve(Long.toString(pageStartMillis / idxExpiryDuration))
        .resolve(streamIdentifier.getStreamId().toString())
        .resolve(baseFileName);
  }

  @Override
  public Path getLogBinDirPath(StreamIdentifier<Id> streamIdentifier, long hrBlock) {
    return dataDir
        .resolve(Long.toString(hrBlock))
        .resolve(streamIdentifier.getStreamId().toString())
        .resolve(baseFileName);
  }

  @Override
  public List<Path> listLogBinFiles(
      StreamIdentifier<Id> streamIdentifier, long fromMillis, long toMillis) {
    var hrStart = fromMillis / idxExpiryDuration;
    var hrEnd = toMillis / idxExpiryDuration;
    return List.of(
        LongStream.rangeClosed(hrStart, hrEnd)
            .mapToObj(
                hr ->
                    dataDir
                        .resolve(Long.toString(hr))
                        .resolve(streamIdentifier.getStreamId().toString())
                        .resolve(baseFileName))
            .toArray(Path[]::new));
  }

  @Override
  public List<BinFileInfo> listPathsInBlock(long hrBlock) throws IOException {
    var hrRoot = dataDir.resolve(Long.toString(hrBlock));
    if (!Files.exists(hrRoot)) {
      return Collections.emptyList();
    }
    // hr / tenant / stream / logfile.bin
    return Files.walk(hrRoot, 3)
        .filter(Files::isRegularFile)
        .filter(p -> p.getFileName().toString().equals(baseFileName))
        .map(
            p -> {
              var stream = p.getParent().getFileName().toString();
              var tenant = p.getParent().getParent().getFileName().toString();
              return new BinFileInfo(tenant, stream, p);
            })
        .toList();
  }

  @Override
  public List<TimestampedBinFile> listAllPaths() throws IOException {
    return Files.walk(dataDir, 4)
        .filter(Files::isRegularFile)
        .filter(p -> p.getFileName().toString().equals(baseFileName))
        .map(
            p -> {
              var stream = p.getParent().getFileName().toString();
              var tenant = p.getParent().getParent().getFileName().toString();
              var hrBlock =
                  Long.parseLong(p.getParent().getParent().getParent().getFileName().toString());
              var timestamp = hrBlock * idxExpiryDuration;
              return new TimestampedBinFile(tenant, stream, p, hrBlock, timestamp);
            })
        .toList();
  }
}
