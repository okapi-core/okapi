package org.okapi.abstractio;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.okapi.streams.StreamIdentifier;

public interface DiskLogBinPaths<Id> {

  Path getLogBinFilePath(StreamIdentifier<Id> streamIdentifier, long pageStartMillis);

  Path getLogBinDirPath(StreamIdentifier<Id> streamIdentifier, long hrBlock);

  List<Path> listLogBinFiles(StreamIdentifier<Id> streamIdentifier, long fromMillis, long toMillis);

  List<BinFileInfo> listPathsInBlock(long hrBlock) throws IOException;

  List<TimestampedBinFile> listAllPaths() throws IOException;

  record BinFileInfo(String tenant, String stream, Path path) {}

  record TimestampedBinFile(String tenant, String stream, Path path, long blk, long timestamp) {}
}
