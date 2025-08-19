package org.okapi.fileutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class OkapiFileUtils {

  public static Optional<Path> getLatestFile(Path root) throws IOException {
    final Path[] mostRecent = {null};
    final long[] mostRecentTime = {-1};

    try (var files = Files.list(root)) {
      files.forEach(
          f -> {
            if (Files.isRegularFile(f)) {
              try {
                long updateTime = Files.getLastModifiedTime(f).toMillis();
                if (updateTime > mostRecentTime[0]) {
                  mostRecent[0] = f;
                  mostRecentTime[0] = updateTime;
                }
              } catch (IOException e) {
                // Optionally log or handle this per file
                throw new RuntimeException(e);
              }
            }
          });
    }

    return Optional.ofNullable(mostRecent[0]);
  }

  public static Optional<Path> getLatestFile(Path root, String prefix, String suffix)
      throws IOException {
    final Path[] mostRecent = {null};
    final long[] mostRecentTime = {-1};

    try (var files = Files.list(root)) {
      files.forEach(
          f -> {
            if (Files.isRegularFile(f)) {
              String fileName = f.getFileName().toString();
              boolean matchesPrefix = prefix == null || fileName.startsWith(prefix);
              boolean matchesSuffix = suffix == null || fileName.endsWith(suffix);

              if (matchesPrefix && matchesSuffix) {
                try {
                  long updateTime = Files.getLastModifiedTime(f).toMillis();
                  if (updateTime > mostRecentTime[0]) {
                    mostRecent[0] = f;
                    mostRecentTime[0] = updateTime;
                  }
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }
            }
          });
    }

    return Optional.ofNullable(mostRecent[0]);
  }
}
