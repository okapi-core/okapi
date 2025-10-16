package org.okapi.logs;

import static org.junit.jupiter.api.Assertions.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.okapi.logs.io.LogPageSerializer;
import org.okapi.logs.io.LogPage;
import org.okapi.logs.s3.S3LogDumpReader;
import org.okapi.protos.logs.LogPayloadProto;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Testcontainers
class S3LogDumpReaderLocalstackIT {
  @Container
  static LocalStackContainer localstack = new LocalStackContainer("localstack/localstack:2.3.2")
      .withServices(LocalStackContainer.Service.S3);

  @Test
  void readsSelectiveSectionsWithRanges() throws Exception {
    // Build a small page
    LogPage page = TestCorpus.buildTestPage();
    byte[] bin = LogPageSerializer.serialize(page);

    String bucket = "okapi-test";
    String key = "logs/tenant/stream/2024010101/logfile.bin";

    S3Client s3 = S3Client.builder()
        .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
        .credentialsProvider(StaticCredentialsProvider
            .create(AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
        .region(Region.of(localstack.getRegion()))
        .build();

    s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
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

