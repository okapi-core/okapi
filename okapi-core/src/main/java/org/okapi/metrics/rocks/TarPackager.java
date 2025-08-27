package org.okapi.metrics.rocks;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TarPackager {

  public static void packageDir(Path dir, Path target) throws IOException {
    if (!Files.exists(target)) {
      Files.createFile(target);
    }
    try (var fos = new FileOutputStream(target.toFile());
        var tos = new TarArchiveOutputStream(fos); ) {
      tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
      addDirectoryToTar(tos, dir.toFile(), "");
    }
  }

  public static void unpackTar(File tarFile, File destDir) throws IOException {
    if (!destDir.exists()) {
      if (!destDir.mkdirs()) {
        throw new IOException("Could not create destination dir: " + destDir);
      }
    }

    try (FileInputStream fis = new FileInputStream(tarFile);
        TarArchiveInputStream tis = new TarArchiveInputStream(fis)) {

      TarArchiveEntry entry;
      while ((entry = tis.getNextTarEntry()) != null) {
        File outFile = new File(destDir, entry.getName());

        if (entry.isDirectory()) {
          if (!outFile.exists() && !outFile.mkdirs()) {
            throw new IOException("Could not create directory: " + outFile);
          }
        } else {
          // ensure parent dirs exist
          File parent = outFile.getParentFile();
          if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create parent dir: " + parent);
          }

          try (FileOutputStream fos = new FileOutputStream(outFile)) {
            tis.transferTo(fos);
          }

          // preserve executable bits & permissions if you want
          outFile.setExecutable((entry.getMode() & 0100) != 0);
          outFile.setReadable(true, false);
          outFile.setWritable(true, true);
        }
      }
    }
  }

  private static void addDirectoryToTar(TarArchiveOutputStream tos, File file, String parent)
      throws IOException {
    String entryName = parent + file.getName();
    TarArchiveEntry entry = new TarArchiveEntry(file, entryName);

    tos.putArchiveEntry(entry);

    if (file.isFile()) {
      try (var fis = Files.newInputStream(file.toPath())) {
        IOUtils.copy(fis, tos);
      }
      tos.closeArchiveEntry();
    } else {
      tos.closeArchiveEntry(); // close directory entry
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          addDirectoryToTar(tos, child, entryName + "/");
        }
      }
    }
  }
}
