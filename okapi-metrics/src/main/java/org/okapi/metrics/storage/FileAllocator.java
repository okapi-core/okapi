package org.okapi.metrics.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class FileAllocator {

  // todo: allocator should pass its own buffers from various levels.
  Path rootPath;

  public FileAllocator(Path rootPath) {
    this.rootPath = rootPath;
  }

  public Path allocate() throws IOException {
    throw new IllegalArgumentException();
    //        var fname = String.format("%d.pages", System.currentTimeMillis());
    //        var fpath = rootPath.resolve(fname);
    //        Files.createFile(fpath);
    //        return fpath;
  }

  public List<Path> list() {
    throw new IllegalArgumentException();
    //        try (var filesStream = Files.list(rootPath)) {
    //            var allPaths = filesStream.toList();
    //            return allPaths.stream().filter(path ->
    // path.toString().endsWith(".slots")).toList();
    //        } catch (IOException e) {
    //            throw new RuntimeException(e);
    //        }
  }
}
