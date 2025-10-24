package org.okapi.logs.query;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.okapi.protos.logs.LogPayloadProto;

class MultiSourceQueryProcessorTest {

  private static LogPayloadProto item(String body) {
    return LogPayloadProto.newBuilder()
        .setDocId(UUID.randomUUID().toString())
        .setTsMillis(1L)
        .setLevel(10)
        .setBody(body)
        .build();
  }

  @Test
  void toggles_sources_basedOnQueryConfig() throws Exception {
    BufferPoolQueryProcessor buffer = mock(BufferPoolQueryProcessor.class);
    OnDiskQueryProcessor disk = mock(OnDiskQueryProcessor.class);
    S3QueryProcessor s3 = mock(S3QueryProcessor.class);
    MemberSetQueryProcessor member = mock(MemberSetQueryProcessor.class);

    when(buffer.getLogs(any(), any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("buf")));
    when(disk.getLogs(any(), any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("disk")));
    when(s3.getLogs(any(), any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("s3")));
    when(member.getLogs(any(), any(), anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(item("member")));

    MultiSourceQueryProcessor m = new MultiSourceQueryProcessor(buffer, disk, s3, member);

    // default config → all sources
    var all = m.getLogs("t", "s", 0, 10, new RegexFilter(".*"), QueryConfig.defaultConfig());
    assertEquals(4, all.size());

    // fan-out → only buffer+disk
    var fan = m.getLogs("t", "s", 0, 10, new RegexFilter(".*"), QueryConfig.fanOutConfig());
    assertEquals(2, fan.size());
    assertTrue(fan.stream().anyMatch(p -> p.getBody().equals("buf")));
    assertTrue(fan.stream().anyMatch(p -> p.getBody().equals("disk")));

    // s3 off
    var cfg1 = new QueryConfig(false, true, true, false);
    var r1 = m.getLogs("t", "s", 0, 10, new RegexFilter(".*"), cfg1);
    assertEquals(3, r1.size());

    // disk off
    var cfg2 = new QueryConfig(true, true, false, false);
    var r2 = m.getLogs("t", "s", 0, 10, new RegexFilter(".*"), cfg2);
    assertEquals(3, r2.size());

    // buffer off
    var cfg3 = new QueryConfig(true, false, true, false);
    var r3 = m.getLogs("t", "s", 0, 10, new RegexFilter(".*"), cfg3);
    assertEquals(3, r3.size());
  }

  @Test
  void propagates_io_exception_from_source() {
    BufferPoolQueryProcessor buffer = mock(BufferPoolQueryProcessor.class);
    OnDiskQueryProcessor disk = mock(OnDiskQueryProcessor.class);
    S3QueryProcessor s3 = mock(S3QueryProcessor.class);
    MemberSetQueryProcessor member = mock(MemberSetQueryProcessor.class);

    try {
      when(buffer.getLogs(any(), any(), anyLong(), anyLong(), any(), any()))
          .thenReturn(List.of());
      when(disk.getLogs(any(), any(), anyLong(), anyLong(), any(), any()))
          .thenThrow(new IOException("disk err"));
      when(s3.getLogs(any(), any(), anyLong(), anyLong(), any(), any()))
          .thenReturn(List.of());
      when(member.getLogs(any(), any(), anyLong(), anyLong(), any(), any()))
          .thenReturn(List.of());

      MultiSourceQueryProcessor m = new MultiSourceQueryProcessor(buffer, disk, s3, member);

      assertThrows(
          IOException.class,
          () -> m.getLogs("t", "s", 0, 10, new RegexFilter(".*"), QueryConfig.defaultConfig()));
    } catch (IOException e) {
      fail(e);
    }
  }
}

