package org.okapi.wal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.Getter;

public class WalAllocator {

  public static final String PREFIX = "wal";
  public static final String SEP = "_";
  public static final String EXTENSION = ".segment";

  private final Path root;

  @Getter private int epoch = 0; // active epoch
  private static final Pattern SEGMENT_PATTERN =
      Pattern.compile("^" + PREFIX + SEP + "\\d{10}" + EXTENSION + "$");

  public WalAllocator(Path root) throws IOException {
    this.root = root;
    Files.createDirectories(root);

    Integer latest = findLatestEpoch(root);
    if (latest == null) {
      this.epoch = 1;
      Files.createFile(getPath(this.epoch));
    } else {
      this.epoch = latest;
    }
  }

  private static Integer findLatestEpoch(Path root) throws IOException {
    try (Stream<Path> s = Files.list(root)) {
      return s.map(p -> p.getFileName().toString())
          .filter(name -> SEGMENT_PATTERN.matcher(name).matches())
          .map(name -> name.substring((PREFIX + SEP).length(), name.length() - EXTENSION.length()))
          .map(Integer::parseInt)
          .max(Comparator.naturalOrder())
          .orElse(null);
    }
  }

  private static int parseEpoch(String filename) {
    int prefixLen = (PREFIX + SEP).length();
    int end = filename.length() - EXTENSION.length();
    String numeric = filename.substring(prefixLen, end);
    return Integer.parseInt(numeric.trim());
  }

  private Path getPath(int epoch) {
    String formatted = String.format("%010d", epoch);
    String fname = PREFIX + SEP + formatted + EXTENSION;
    return root.resolve(fname);
  }

  public synchronized Path allocate() throws IOException {
    int next = this.epoch + 1;
    Path path = getPath(next);
    Files.createFile(path);
    this.epoch = next;
    return path;
  }

  public synchronized Path active() {
    return getPath(this.epoch);
  }
}
