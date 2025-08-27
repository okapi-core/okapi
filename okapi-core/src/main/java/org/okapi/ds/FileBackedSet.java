package org.okapi.ds;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileBackedSet implements PersistedSet<String> {
  public static final String EOC_MARKER = "END";
  public static final byte[] NEW_LINE = "\n".getBytes();
  public static final byte[] EOC_MARKER_BYTES = " END".getBytes();
  public static final byte[] CMD_ADD = "ADD ".getBytes();
  public static final byte[] CMD_RM = "RM ".getBytes();
  Path wal;
  Map<String, Boolean> contents;

  enum STATE {
    READY,
    TORN,
    CLOSED
  }

  STATE state;

  public FileBackedSet(Path wal) throws IOException {
    if (!Files.exists(wal)) {
      Files.createFile(wal);
    }
    contents = new ConcurrentHashMap<>();
    this.wal = wal;
    this.replay(wal);
  }

  private void replay(Path file) {
    try (var br = new BufferedReader(new FileReader(file.toFile()))) {
      String line;
      while ((line = br.readLine()) != null) {
        var cmds = line.split(" ");
        if (cmds.length != 3) {
          log.error("Torn wal, stopping replay.");
          state = STATE.TORN;
          return;
        }
        var cmd = cmds[0];
        var el = cmds[1];
        var eocMarker = cmds[2];
        if (!eocMarker.equals(EOC_MARKER)) continue;
        if (cmd.equals("ADD")) {
          contents.put(el, true);
        } else if (cmd.equals("RM")) {
          contents.remove(el);
        } else {
          log.error("Unknown command {}", line);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void write(byte[] contents) throws IOException {
    Files.write(wal, contents, StandardOpenOption.APPEND);
  }

  @Override
  public void add(String el) throws IOException {
    if (state == STATE.CLOSED) {
      throw new IllegalStateException("Cannot write to a closed set.");
    }
    if (!contents.isEmpty()) {
      write("\n".getBytes());
    }
    write(CMD_ADD);
    write(el.getBytes());
    write(EOC_MARKER_BYTES);
    contents.put(el, true);
  }

  @Override
  public void remove(String el) throws IOException {
    if (state == STATE.CLOSED) {
      throw new IllegalStateException("Cannot delete from a closed set.");
    }
    if (!contents.isEmpty()) {
      write(NEW_LINE);
    }
    write(CMD_RM);
    write(el.getBytes());
    write(EOC_MARKER_BYTES);
    contents.remove(el);
  }

  @Override
  public boolean contains(String el) {
    return contents.containsKey(el);
  }

  @Override
  public void flush() throws IOException {
    FileChannel.open(this.wal).force(true);
  }

  @Override
  public Set<String> list() {
    return Collections.unmodifiableSet(this.contents.keySet());
  }

  @Override
  public void close() throws IOException {
  }
}
