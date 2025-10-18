package org.okapi.traces.testutil;

import java.util.List;
import org.okapi.traces.query.S3TracefileKeyResolver;

/**
 * Minimal S3 key resolver for tests: key format is "tenant/application/hourBucket".
 */
public class TestS3Resolver implements S3TracefileKeyResolver {
  private final String bucket;

  public TestS3Resolver(String bucket) {
    this.bucket = bucket;
  }

  @Override
  public String bucket() {
    return bucket;
  }

  @Override
  public List<String> keyFor(String tenant, String application, long hourBucket) {
    return java.util.List.of(tenant + "/" + application + "/" + hourBucket + "/");
  }

  @Override
  public String uploadKey(String tenant, String application, long hourBucket, String nodeId) {
    return tenant + "/" + application + "/" + hourBucket + "/" + nodeId + "/tracefile.bin";
  }
}
