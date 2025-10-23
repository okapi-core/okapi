package org.okapi.logs;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.okapi.logs.config.LogsCfgImpl;
import org.okapi.logs.forwarding.LogForwarder;
import org.okapi.logs.io.LogPage;
import org.okapi.logs.io.LogPageSerializer;
import org.okapi.logs.query.RegexFilter;
import org.okapi.logs.query.S3QueryProcessor;
import org.okapi.logs.query.TraceFilter;
import org.okapi.logs.spring.AwsConfiguration;
import org.okapi.protos.logs.LogPayloadProto;
import org.okapi.swim.membership.MembershipEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest(classes = {TestApplication.class, AwsConfiguration.class})
@TestPropertySource(
    properties = {"okapi.logs.s3Bucket=unit-bucket", "okapi.logs.s3BasePrefix=logs"})
@ActiveProfiles("test")
class S3QueryIntegrationTest {
  @Autowired LogsCfgImpl cfg;

  @Mock S3Client s3Client;

  @MockitoBean LogForwarder logForwarder;

  @MockitoBean MembershipEventPublisher membershipEventPublisher;

  @Test
  void queryFromS3Dump() throws Exception {
    // Build a single-page dump with 10 docs
    LogPage page = TestCorpus.buildTestPage();
    byte[] bin = LogPageSerializer.serialize(page);
    byte[] idx =
        buildSingleIndex(
            bin.length,
            page.getTsStart(),
            page.getTsEnd(),
            page.getMaxDocId() + 1,
            Ints.fromByteArray(copy(bin, bin.length - 4, 4)));

    Map<String, byte[]> store = new HashMap<>();
    long startTs = page.getTsStart();
    var hr = startTs / cfg.getIdxExpiryDuration();
    String prefix = cfg.getS3BasePrefix() + "/tenantX/streamY/" + hr + "/node-1";
    store.put(prefix + "/logfile.idx", idx);
    store.put(prefix + "/logfile.bin", bin);

    S3QueryProcessor qp = new FakeS3QueryProcessor(cfg, store, s3Client);
    List<LogPayloadProto> traceA =
        qp.getLogs(
            "tenantX",
            "streamY",
            startTs - 60000,
            startTs + 60000,
            new TraceFilter("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
            org.okapi.logs.query.QueryConfig.defaultConfig());
    assertEquals(5, traceA.size());
    List<LogPayloadProto> failed =
        qp.getLogs(
            "tenantX",
            "streamY",
            startTs - 60000,
            startTs + 60000,
            new RegexFilter("failed"),
            org.okapi.logs.query.QueryConfig.defaultConfig());
    assertEquals(2, failed.size());
  }

  private static byte[] buildSingleIndex(
      int length, long tsStart, long tsEnd, int docCount, int crc) {
    List<byte[]> parts = new ArrayList<>();
    parts.add(Longs.toByteArray(0L));
    parts.add(Ints.toByteArray(length));
    parts.add(Longs.toByteArray(tsStart));
    parts.add(Longs.toByteArray(tsEnd));
    parts.add(Ints.toByteArray(docCount));
    parts.add(Ints.toByteArray(crc));
    int total = parts.stream().mapToInt(a -> a.length).sum();
    byte[] out = new byte[total];
    int p = 0;
    for (byte[] a : parts) {
      System.arraycopy(a, 0, out, p, a.length);
      p += a.length;
    }
    return out;
  }

  private static byte[] copy(byte[] a, int off, int len) {
    byte[] r = new byte[len];
    System.arraycopy(a, off, r, 0, len);
    return r;
  }

  static class FakeS3QueryProcessor extends S3QueryProcessor {
    private final Map<String, byte[]> store;

    FakeS3QueryProcessor(LogsCfgImpl cfg, Map<String, byte[]> store, S3Client client) {
      super(cfg, new io.micrometer.core.instrument.simple.SimpleMeterRegistry(), client);
      this.store = store;
    }

    @Override
    protected byte[] getObjectBytes(String bucket, String key) {
      return store.get(key);
    }

    @Override
    protected byte[] getRangeBytes(String bucket, String key, long offset, int length) {
      byte[] all = store.get(key);
      byte[] r = new byte[length];
      System.arraycopy(all, (int) offset, r, 0, length);
      return r;
    }

    @Override
    protected java.util.List<String> listObjectKeys(String bucket, String prefix) {
      java.util.List<String> keys = new java.util.ArrayList<>();
      for (String k : store.keySet()) if (k.startsWith(prefix)) keys.add(k);
      return keys;
    }
  }
}
