package org.okapi.logs.query;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.okapi.common.CommonConfigs.getQueryCfg;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.okapi.logs.query.processor.*;
import org.okapi.primitives.BinaryLogRecordV1;

class MultiSourceLogsQueryProcessorTest {

  private static BinaryLogRecordV1 item(String body) {
    return BinaryLogRecordV1.builder()
        .docId(RandomStringUtils.secure().next(10))
        .tsMillis(1L)
        .level(10)
        .body(body)
        .build();
  }

  @Test
  void toggles_sources_basedOnQueryConfig() throws Exception {
    BufferPoolLogsQueryProcessor buffer = mock(BufferPoolLogsQueryProcessor.class);
    OnDiskLogsQueryProcessor disk = mock(OnDiskLogsQueryProcessor.class);
    S3LogsQueryProcessor s3 = mock(S3LogsQueryProcessor.class);
    PeersLogsQueryProcessor member = mock(PeersLogsQueryProcessor.class);

    when(buffer.getLogs(any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("buf")));
    when(disk.getLogs(any(), anyLong(), anyLong(), any(), any())).thenReturn(List.of(item("disk")));
    when(s3.getLogs(any(), anyLong(), anyLong(), any(), any())).thenReturn(List.of(item("s3")));
    when(member.getLogs(any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("member")));

    MultiSourceLogsQueryProcessor m =
        new MultiSourceLogsQueryProcessor(buffer, disk, s3, member, getQueryCfg());

    // default config → all sources
    var all = m.getLogs("s", 0, 10, new RegexPageFilter(".*"), QueryConfig.allSources());
    assertEquals(4, all.size());

    // fan-out → only buffer+disk
    var fan = m.getLogs("s", 0, 10, new RegexPageFilter(".*"), QueryConfig.localSources());
    assertEquals(2, fan.size());
    assertTrue(fan.stream().anyMatch(p -> p.getBody().equals("buf")));
    assertTrue(fan.stream().anyMatch(p -> p.getBody().equals("disk")));

    // s3 off
    var cfg1 = new QueryConfig(false, true, true, false);
    var r1 = m.getLogs("s", 0, 10, new RegexPageFilter(".*"), cfg1);
    assertEquals(2, r1.size());

    // disk off
    var cfg2 = new QueryConfig(true, true, false, false);
    var r2 = m.getLogs("s", 0, 10, new RegexPageFilter(".*"), cfg2);
    assertEquals(2, r2.size());

    // buffer off
    var cfg3 = new QueryConfig(true, false, true, false);
    var r3 = m.getLogs("s", 0, 10, new RegexPageFilter(".*"), cfg3);
    assertEquals(2, r3.size());
  }

  @Test
  void propagates_io_exception_from_source() {
    BufferPoolLogsQueryProcessor buffer = mock(BufferPoolLogsQueryProcessor.class);
    OnDiskLogsQueryProcessor disk = mock(OnDiskLogsQueryProcessor.class);
    S3LogsQueryProcessor s3 = mock(S3LogsQueryProcessor.class);
    PeersLogsQueryProcessor member = mock(PeersLogsQueryProcessor.class);

    try {
      when(buffer.getLogs(any(), anyLong(), anyLong(), any(), any())).thenReturn(List.of());
      when(disk.getLogs(any(), anyLong(), anyLong(), any(), any()))
          .thenThrow(new IOException("disk err"));
      when(s3.getLogs(any(), anyLong(), anyLong(), any(), any())).thenReturn(List.of());
      when(member.getLogs(any(), anyLong(), anyLong(), any(), any())).thenReturn(List.of());

      MultiSourceLogsQueryProcessor m =
          new MultiSourceLogsQueryProcessor(buffer, disk, s3, member, getQueryCfg());

      assertThrows(
          CompletionException.class,
          () -> m.getLogs("s", 0, 10, new RegexPageFilter(".*"), QueryConfig.localSources()));
    } catch (IOException e) {
      fail(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
