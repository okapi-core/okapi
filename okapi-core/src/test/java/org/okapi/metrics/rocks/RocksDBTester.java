package org.okapi.metrics.rocks;

import com.google.common.primitives.Longs;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.*;

public class RocksDBTester {
    @TempDir
    Path temp;

    @Test
    public void testWithRocks() throws RocksDBException {
        var rocksDb = RocksDB.open(temp.toString());
        var labels1 = "label:metric_path";
        var labels2 = "label:metric_path_2";
        var time = System.currentTimeMillis();
        rocksDb.put(labels1.getBytes(), Longs.toByteArray(time));
        rocksDb.put(labels2.getBytes(), Longs.toByteArray(time));

    }
}
