package org.okapi.nodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileBackedIdSupplierTests {

  @TempDir Path tempDir;

  @Test
  void testPersistsGeneratedId() throws IOException {
    var file = tempDir.resolve("node-id.txt");
    var supplier = new FileBackedIdSupplier(file);
    var firstId = supplier.getNodeId();
    var persisted = Files.readString(file).trim();
    assertEquals(firstId, persisted);

    var secondId = new FileBackedIdSupplier(file).getNodeId();
    assertEquals(firstId, secondId);
  }

  @Test
  void testReadsExisting() throws IOException {
    var file = tempDir.resolve("node-id.txt");
    Files.write(file, "existing-node-id".getBytes());
    var supplier = new FileBackedIdSupplier(file);
    assertEquals("existing-node-id", supplier.getNodeId());
  }

  @Test
  void testFailsFastWhenFileIsBlank() throws IOException {
    var file = tempDir.resolve("node-id.txt");
    Files.createFile(file);
    file.toFile().setWritable(false);
    assertThrows(IOException.class, () -> new FileBackedIdSupplier(file));
  }

  @Test
  void testFailsFastWhenFileIsNotWritable() throws IOException {
    var file = tempDir.resolve("node-id.txt");
    Files.createFile(file);
    file.toFile().setWritable(false);
    assertThrows(IOException.class, () -> new FileBackedIdSupplier(file));
  }
}
