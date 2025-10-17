package org.okapi.logs;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.okapi.logs.io.LogPage;
import org.okapi.logs.io.LogPageSerializer;

class LogPageSerializerTest {
  @Test
  void roundTrip_withCrc32() throws Exception {
    LogPage page =
        LogPage.builder()
            .traceIdSet(BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 100))
            .expectedInsertions(100)
            .build();

    page.append(1000L, "trace-1", 20, "hello info one");
    page.append(1001L, "trace-1", 40, "error fail one");
    page.append(1002L, "trace-2", 30, "warn two");

    byte[] bytes = LogPageSerializer.serialize(page);
    assertTrue(bytes.length > 80);

    LogPage rt = LogPageSerializer.deserialize(bytes);
    assertEquals(page.getTsStart(), rt.getTsStart());
    assertEquals(page.getTsEnd(), rt.getTsEnd());
    assertEquals(page.getMaxDocId(), rt.getMaxDocId());
    assertEquals(page.getLogDocs().size(), rt.getLogDocs().size());
    assertEquals(page.getLevelsInPage().keySet(), rt.getLevelsInPage().keySet());
    assertEquals(page.getTrigramMap().size(), rt.getTrigramMap().size());
  }
}
