package org.okapi.disk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.byterange.DiskByteRangeSupplier;

public class DiskByteRangeSupplierTests {
  Path fp;

  @BeforeEach
  void setupCorpus() throws IOException {
    fp = Path.of("./test-corpus-A.txt");
    Files.write(fp, "test-corpus-A".getBytes());
  }

  @Test
  void testByteAccess() throws Exception {
    var diskSupplier = new DiskByteRangeSupplier(fp);
    var bytes = diskSupplier.getBytes(0, 4);
    assertEquals("test", new String(bytes));

    var singleByte = diskSupplier.getBytes(5, 1);
    assertEquals("c", new String(singleByte));

    var noBytes = diskSupplier.getBytes(0, 0);
    assertEquals(0, noBytes.length);
    diskSupplier.close();
  }
  
  @AfterEach
  void teardDown() throws IOException {
    Files.delete(fp);
  }
}
