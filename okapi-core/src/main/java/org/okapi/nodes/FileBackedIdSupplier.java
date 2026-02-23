package org.okapi.nodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.Getter;

public class FileBackedIdSupplier implements NodeIdSupplier {
  @Getter Path filePath;
  @Getter String nodeId;

  public FileBackedIdSupplier(Path filePath) throws IOException {
    this.filePath = filePath;
    if (Files.exists(filePath)) {
      nodeId = readIdFromDisk();
    } else {
      nodeId = UUID.randomUUID().toString();
      writeId();
    }
  }

  public void writeId() throws IOException {
    Files.writeString(filePath, nodeId);
  }

  public String readIdFromDisk() throws IOException {
    var id = Files.readString(filePath).trim();
    if (id.isEmpty()) {
      throw new IOException("Node ID file is empty: " + filePath + " ,please delete it first.");
    }
    return id;
  }
}
