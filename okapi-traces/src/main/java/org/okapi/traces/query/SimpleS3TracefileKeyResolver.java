package org.okapi.traces.query;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SimpleS3TracefileKeyResolver implements S3TracefileKeyResolver {
  private final String bucket;
  private final String basePrefix; // e.g., "okapi"

  @Override
  public String bucket() { return bucket; }

  @Override
  public String keyFor(String tenantId, String application, long hourBucket) {
    String prefix = basePrefix == null || basePrefix.isEmpty() ? "" : (basePrefix.endsWith("/") ? basePrefix : basePrefix + "/");
    return prefix + tenantId + "/" + application + "/tracefile." + hourBucket + ".bin";
  }
}

