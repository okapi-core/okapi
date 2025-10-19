package org.okapi.logs;

import static org.junit.jupiter.api.Assertions.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.okapi.logs.io.LogPageSerializer;
import org.okapi.logs.io.LogPage;
import org.okapi.logs.s3.S3LogDumpReader;
import org.okapi.protos.logs.LogPayloadProto;
import org.okapi.logs.testutil.S3TestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3LogDumpReaderLocalstackIT {
  @Test
  void readsSelectiveSectionsWithRanges() throws Exception {
    // Build a small page
    LogPage page = TestCorpus.buildTestPage();
    byte[] bin = LogPageSerializer.serialize(page);

    String bucket = "okapi-test";
    String key = "logs/tenant/stream/2024010101/logfile.bin";

    S3Client s3 = S3TestUtils.createS3Client();
    S3TestUtils.ensureBucketExists(s3, bucket);
    s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
        software.amazon.awssdk.core.sync.RequestBody.fromBytes(bin));

    S3LogDumpReader reader = new S3LogDumpReader(s3, new SimpleMeterRegistry());
    S3LogDumpReader.Header hdr = reader.readHeader(bucket, key, 0);
    assertTrue(hdr.lenDocs() > 0);

    // Read doc sizes and fetch first doc
    byte[] sizes = reader.readDocsSizes(bucket, key, 0, hdr);
    List<LogPayloadProto> docs = reader.readDocsByIds(bucket, key, 0, hdr, new int[] {0, 1});
    assertEquals(2, docs.size());
  }
}
