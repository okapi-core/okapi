package org.okapi.fileutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OkapiFileUtilsTest {
  private Path tempDir;

  @BeforeEach
  void setup() throws IOException {
    tempDir = Files.createTempDirectory("testdir");
  }

  @AfterEach
  void cleanup() throws IOException {
    Files.walk(tempDir)
        .sorted((a, b) -> b.compareTo(a)) // delete children before parent
        .forEach(
            path -> {
              try {
                Files.deleteIfExists(path);
              } catch (IOException e) {
                // ignore
              }
            });
  }

  @Test
  void testGetLatestFile_singleFile() throws IOException {
    Path file = Files.createTempFile(tempDir, "file", ".txt");
    Optional<Path> result = OkapiFileUtils.getLatestFile(tempDir);
    assertTrue(result.isPresent());
    assertEquals(file, result.get());
  }

  @Test
  void testGetLatestFile_multipleFiles() throws IOException {
    Path oldFile = Files.createTempFile(tempDir, "old", ".txt");
    try {
      Thread.sleep(10);
    } catch (InterruptedException ignored) {
    }
    Path newFile = Files.createTempFile(tempDir, "new", ".txt");

    Optional<Path> result = OkapiFileUtils.getLatestFile(tempDir);
    assertTrue(result.isPresent());
    assertEquals(newFile, result.get());
  }

  @Test
  void testGetLatestFile_ignoresDirectories() throws IOException {
    Files.createDirectory(tempDir.resolve("subdir"));
    Optional<Path> result = OkapiFileUtils.getLatestFile(tempDir);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetLatestFile_emptyDirectory() throws IOException {
    Optional<Path> result = OkapiFileUtils.getLatestFile(tempDir);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetLatestFile_mixedContent() throws IOException {
    Path file1 = Files.createTempFile(tempDir, "f1", ".txt");
    Files.createDirectory(tempDir.resolve("folder"));
    try {
      Thread.sleep(10);
    } catch (InterruptedException ignored) {
    }
    Path file2 = Files.createTempFile(tempDir, "f2", ".log");

    Optional<Path> result = OkapiFileUtils.getLatestFile(tempDir);
    assertTrue(result.isPresent());
    assertEquals(file2, result.get());
  }

  @Test
  void testGetLatestFile_withPrefixMatchOnly() throws IOException {
    Path file1 = Files.createTempFile(tempDir, "abc_log_", ".txt");
    Files.createTempFile(tempDir, "no_match", ".txt");

    Optional<Path> result = OkapiFileUtils.getLatestFile(tempDir, "abc_log_", null);
    assertTrue(result.isPresent());
    assertEquals(file1.getFileName(), result.get().getFileName());
  }

  @Test
  void testGetLatestFile_withSuffixMatchOnly() throws IOException {
    Files.createTempFile(tempDir, "file1", ".log");
    Path file2 = Files.createTempFile(tempDir, "file2", ".txt");

    Optional<Path> result = OkapiFileUtils.getLatestFile(tempDir, null, ".txt");
    assertTrue(result.isPresent());
    assertEquals(file2.getFileName(), result.get().getFileName());
  }

  @Test
  void testGetLatestFile_withPrefixAndSuffixMatch() throws IOException {
    Files.createTempFile(tempDir, "prefix_ignore_", ".txt");
    Path match = Files.createTempFile(tempDir, "prefix_target_", ".log");

    Optional<Path> result = OkapiFileUtils.getLatestFile(tempDir, "prefix_", ".log");
    assertTrue(result.isPresent());
    assertEquals(match.getFileName(), result.get().getFileName());
  }

  @Test
  void testGetLatestFile_withNoMatchingFiles() throws IOException {
    Files.createTempFile(tempDir, "alpha", ".dat");
    Files.createTempFile(tempDir, "beta", ".bin");

    Optional<Path> result = OkapiFileUtils.getLatestFile(tempDir, "log_", ".txt");
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetLatestFile_withNullPrefixAndSuffix() throws IOException {
    Path file = Files.createTempFile(tempDir, "anything", ".any");
    Optional<Path> result = OkapiFileUtils.getLatestFile(tempDir, null, null);
    assertTrue(result.isPresent());
    assertEquals(file.getFileName(), result.get().getFileName());
  }
}
