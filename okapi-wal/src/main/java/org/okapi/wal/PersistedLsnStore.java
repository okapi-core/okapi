package org.okapi.wal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Source of truth for the last fsynced (durable) LSN. Atomic write via temp + rename. Thread-safe
 * for simple use.
 */
public final class PersistedLsnStore {
  private static final String FILE_NAME = "persisted.lsn";
  private static final String TMP_SUFFIX = ".tmp";

  private final Path file;
  private final Path tmp;

  private PersistedLsnStore(Path root) {
    this.file = root.resolve(FILE_NAME);
    this.tmp = root.resolve(FILE_NAME + TMP_SUFFIX);
  }

  public static PersistedLsnStore open(Path root) throws IOException {
    Files.createDirectories(root);
    return new PersistedLsnStore(root);
  }

  /** Returns -1 if file missing or empty/invalid. */
  public long read() throws IOException {
    if (!Files.exists(file)) return -1L;
    try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      String line = r.readLine();
      if (line == null) return -1L;
      line = line.trim();
      if (line.isEmpty()) return -1L;
      return Long.parseLong(line);
    } catch (NumberFormatException e) {
      return -1L;
    }
  }

  public void write(long lsn) throws IOException {
    // write tmp
    try (BufferedWriter w =
        Files.newBufferedWriter(
            tmp,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE)) {
      w.write(Long.toString(lsn));
      w.write('\n');
    }
    // atomic move
    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  /** Writes new value only if greater than current. */
  public boolean updateIfGreater(long lsn) throws IOException {
    long cur = read();
    if (lsn > cur) {
      write(lsn);
      return true;
    }
    return false;
  }
}
