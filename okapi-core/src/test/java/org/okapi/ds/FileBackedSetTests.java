package org.okapi.ds;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileBackedSetTests {

    @TempDir
    Path temp;
    Path wal;

    @BeforeEach
    public void setup(){
        wal = temp.resolve("wal");
    }
    @Test
    public void testWriteHappyPath() throws IOException {
        var fbs = new FileBackedSet(wal);
        fbs.add("new-path{}");
        fbs.add("new-path-2{}");
        
        assertTrue(fbs.contains("new-path{}"));
        assertTrue(fbs.contains("new-path-2{}"));
    }

    @Test
    public void testRestoreHappyPath() throws IOException {
        var cmds = "ADD new-path{} END\nADD old-path{} END\n";
        Files.createFile(wal);
        Files.write(wal, cmds.getBytes(), StandardOpenOption.APPEND);
        var fbs = new FileBackedSet(wal);
        assertTrue(fbs.contains("new-path{}"));
        assertTrue(fbs.contains("old-path{}"));
    }

    @Test
    public void testRestore_TornWrite() throws IOException {
        var cmds = "ADD new-path{} END\nADD old-path{}\n";
        Files.createFile(wal);
        Files.write(wal, cmds.getBytes(), StandardOpenOption.APPEND);
        var fbs = new FileBackedSet(wal);
        assertTrue(fbs.contains("new-path{}"));
        assertFalse(fbs.contains("old-path{}"));
    }

    @Test
    public void testRestore_withDelete() throws IOException {
        var cmds = "ADD new-path{} END\nADD old-path{} END\nRM old-path{} END\n";
        Files.createFile(wal);
        Files.write(wal, cmds.getBytes(), StandardOpenOption.APPEND);
        var fbs = new FileBackedSet(wal);
        assertTrue(fbs.contains("new-path{}"));
        assertFalse(fbs.contains("old-path{}"));
    }
    @Test
    public void testRestore_withDeleteTorn() throws IOException {
        var cmds = "ADD new-path{} END\nADD old-path{} END\nRM old-path{}";
        Files.createFile(wal);
        Files.write(wal, cmds.getBytes(), StandardOpenOption.APPEND);
        var fbs = new FileBackedSet(wal);
        assertTrue(fbs.contains("new-path{}"));
        assertTrue(fbs.contains("old-path{}"));
    }


    @Test
    public void testRestore_withTruncation() throws IOException {
        var cmds = "ADD new-path{} END\nADD old-path{} END\nRM old-path{}\nADD new-path-2{} END\n";
        Files.createFile(wal);
        Files.write(wal, cmds.getBytes(), StandardOpenOption.APPEND);
        var fbs = new FileBackedSet(wal);
        assertTrue(fbs.contains("new-path{}"));
        assertTrue(fbs.contains("old-path{}"));
        assertFalse(fbs.contains("new-path-2{}"));
    }

    @Test
    public void testRestoreFromAnother() throws IOException {
        var fbs = new FileBackedSet(wal);
        fbs.add("path-1{}");
        fbs.add("path-2{}");
        fbs.flush();
        
        var restored = new FileBackedSet(wal);
        assertTrue(restored.contains("path-1{}"));
        assertTrue(restored.contains("path-2{}"));
    }
}
