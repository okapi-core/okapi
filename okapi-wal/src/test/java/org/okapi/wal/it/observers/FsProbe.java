package org.okapi.wal.it.observers;

import org.okapi.wal.SegmentIndex;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FsProbe {
  private FsProbe() {}

  public static long size(Path p) throws IOException {
    return Files.size(p);
  }

  public static List<Path> listSegments(Path root) throws IOException {
    try (Stream<Path> s = Files.list(root)) {
      return s.filter(p -> p.getFileName().toString().matches("^wal_\\d{10}\\.segment$"))
          .sorted(Comparator.comparingInt(SegmentIndex::parseEpochFromSegment))
          .collect(Collectors.toList());
    }
  }

  public static Path trashRoot(Path root) {
    return root.resolve(".wal_trash");
  }

  public static boolean exists(Path p) {
    return Files.exists(p);
  }
}
