package org.okapi.wal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * Simple single-process lock implemented via atomic file creation.
 * - Creates {root}/{name}.lock with CREATE_NEW; fails if it exists.
 * - Writes small diagnostic info into the lock file.
 * - Close() deletes the lock file (idempotent).
 */
public final class WalProcessLock implements AutoCloseable {

    private final Path lockPath;
    private volatile boolean held;

    private WalProcessLock(Path lockPath) {
        this.lockPath = lockPath;
        this.held = true;
    }

    public static WalProcessLock acquire(Path root, String name, Map<String, String> info) throws IOException {
        Path lockPath = root.resolve(name + ".lock");
        try {
            Files.writeString(
                    lockPath,
                    formatInfo(info),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            // ensure meaningful message
            throw new IOException("Failed to acquire " + name + " lock at " + lockPath + " (already held?)", e);
        }
        return new WalProcessLock(lockPath);
    }

    public boolean isHeld() {
        return held;
    }

    @Override
    public void close() throws IOException {
        if (!held) return;
        try {
            Files.deleteIfExists(lockPath);
        } finally {
            held = false;
        }
    }

    private static String formatInfo(Map<String, String> info) {
        StringBuilder sb = new StringBuilder();
        if (info != null) {
            info.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));
        }
        return sb.toString();
    }
}
