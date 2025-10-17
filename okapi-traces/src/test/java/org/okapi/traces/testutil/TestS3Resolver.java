package org.okapi.traces.testutil;

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
  public String keyFor(String tenant, String application, long hourBucket) {
    return tenant + "/" + application + "/" + hourBucket;
  }
}

