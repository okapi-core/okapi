/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.checksums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.hash.Hashing;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ChecksumUtilsTest {
  @TempDir Path tempDir;

  @Test
  void testChecksumComputation() throws IOException {
    var filePath = tempDir.resolve("testfile.txt");
    var content = "This is a test file for checksum computation.";
    Files.write(filePath, content.getBytes());
    var checkSum = ChecksumUtils.getChecksum(filePath);
    // Expected value computed using the SAME Guava murmur3_128 implementation
    var expectedChecksum = Hashing.murmur3_128().hashBytes(content.getBytes()).toString();
    assertEquals(expectedChecksum, checkSum);
  }
}
