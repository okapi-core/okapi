package org.okapi.metrics.rocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.testutils.OkapiTestUtils;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class TarPackagerTests {

  @TempDir Path packageDir;

  @TempDir Path unpackDir;

  Path tarFile;

  @TempDir Path rocksRoot;
  @TempDir Path rocksUnpackRoot;

  @BeforeEach
  public void setup() throws IOException {
    tarFile = Files.createTempFile("test", ".tar");
  }

  @Test
  public void testTarPackaging_singleFile() throws IOException {
    var root = packageDir.resolve("rocks");
    Files.createDirectories(root);
    var file1 = root.resolve("file-a.txt");
    Files.write(file1, "contents".getBytes());

    // pack
    TarPackager.packageDir(root, tarFile);

    // unpack
    TarPackager.unpackTar(tarFile.toFile(), unpackDir.toFile());
    var unpackRoot = unpackDir.resolve("rocks");

    assertTrue(Files.exists(unpackRoot.resolve("file-a.txt")));
  }

  @Test
  public void testTarPackaging_twoFiles() throws IOException {
    var root = packageDir.resolve("rocks");
    Files.createDirectories(root);
    var file1 = root.resolve("file-a.txt");
    var file2 = root.resolve("file-b.txt");
    Files.write(file1, "contents-1".getBytes());
    Files.write(file2, "contents-2".getBytes());

    // pack
    TarPackager.packageDir(root, tarFile);

    // unpack
    TarPackager.unpackTar(tarFile.toFile(), unpackDir.toFile());
    var unpackRoot = unpackDir.resolve("rocks");

    assertTrue(Files.exists(unpackRoot.resolve("file-a.txt")));
    assertTrue(Files.exists(unpackRoot.resolve("file-b.txt")));
    assertEquals("contents-1", Files.readString(unpackRoot.resolve("file-a.txt")));
    assertEquals("contents-2", Files.readString(unpackRoot.resolve("file-b.txt")));
  }

  @Test
  public void testTarPackaging_nested() throws IOException {
    var root = packageDir.resolve("rocks");
    Files.createDirectories(root);
    var file1 = root.resolve("file-a.txt");
    var file2 = root.resolve("nested/file.txt");
    Files.createDirectories(root.resolve("nested"));
    Files.write(file1, "contents-1".getBytes());
    Files.write(file2, "contents-2".getBytes());
    // pack
    TarPackager.packageDir(root, tarFile);

    // unpack
    TarPackager.unpackTar(tarFile.toFile(), unpackDir.toFile());
    var unpackRoot = unpackDir.resolve("rocks");
    assertTrue(Files.exists(unpackRoot.resolve("file-a.txt")));
    assertTrue(Files.exists(unpackRoot.resolve("nested/file.txt")));
    assertEquals("contents-1", Files.readString(unpackRoot.resolve("file-a.txt")));
    assertEquals("contents-2", Files.readString(unpackRoot.resolve("nested/file.txt")));
  }

  @Test
  public void testRocksPortability() throws RocksDBException, IOException {
    var rocksPath = rocksRoot.resolve("123");
    var rocks = RocksDB.open(rocksPath.toString());
    rocks.put("key".getBytes(), "value".getBytes());
    rocks.flushWal(true);
    // pack
    TarPackager.packageDir(rocksPath, tarFile);
    // unpack
    TarPackager.unpackTar(tarFile.toFile(), rocksUnpackRoot.toFile());

    assertTrue(Files.exists(rocksUnpackRoot.resolve("123")));
    var rocksUnpack = RocksDB.open(rocksUnpackRoot.resolve("123").toString());
    var value = rocksUnpack.get("key".getBytes());
    assertTrue(OkapiTestUtils.bytesAreEqual("value".getBytes(), value));
  }
}
