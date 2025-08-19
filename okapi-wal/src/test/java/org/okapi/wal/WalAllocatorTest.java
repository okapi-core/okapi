package org.okapi.wal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class WalAllocatorTest {

    @TempDir Path tmp;

    @Test
    void freshBootstrapping_createsFirstSegmentAndActive() throws Exception {
        WalAllocator alloc = new WalAllocator(tmp);
        Path active = alloc.active();

        assertThat(Files.exists(active)).isTrue();
        assertThat(active.getFileName().toString()).isEqualTo("wal_0000000001.segment");
    }

    @Test
    void latestDiscovery_usesHighestEpoch() throws Exception {
        Files.createFile(tmp.resolve("wal_0000000001.segment"));
        Files.createFile(tmp.resolve("wal_0000000003.segment"));

        WalAllocator alloc = new WalAllocator(tmp);

        assertThat(alloc.active().getFileName().toString()).isEqualTo("wal_0000000003.segment");
        Path next = alloc.allocate();
        assertThat(next.getFileName().toString()).isEqualTo("wal_0000000004.segment");
    }

    @Test
    void monotonicAllocation_createsSequentialFiles() throws Exception {
        WalAllocator alloc = new WalAllocator(tmp);
        Path s1 = alloc.active();
        Path s2 = alloc.allocate();
        Path s3 = alloc.allocate();

        assertThat(s1.getFileName().toString()).isEqualTo("wal_0000000001.segment");
        assertThat(s2.getFileName().toString()).isEqualTo("wal_0000000002.segment");
        assertThat(s3.getFileName().toString()).isEqualTo("wal_0000000003.segment");

        assertThat(Files.exists(s2)).isTrue();
        assertThat(Files.exists(s3)).isTrue();
        assertThat(alloc.active()).isEqualTo(s3);
    }

    @Test
    void ignoresNonMatchingFiles() throws Exception {
        Files.createFile(tmp.resolve("foo.txt"));
        Files.createFile(tmp.resolve("wal_broken.segment"));
        Files.createFile(tmp.resolve("wal_0000000007.segment"));

        WalAllocator alloc = new WalAllocator(tmp);
        assertThat(alloc.active().getFileName().toString()).isEqualTo("wal_0000000007.segment");
    }
}
