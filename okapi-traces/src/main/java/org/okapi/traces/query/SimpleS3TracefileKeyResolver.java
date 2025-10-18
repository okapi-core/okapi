package org.okapi.traces.query;

import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SimpleS3TracefileKeyResolver implements S3TracefileKeyResolver {
  private final String bucket;
  private final String basePrefix; // e.g., "okapi"

  @Override
  public String bucket() {
    return bucket;
  }

  private String basePath(String tenantId, String application, long hourBucket) {
    String bp =
        basePrefix == null || basePrefix.isEmpty()
            ? ""
            : (basePrefix.endsWith("/") ? basePrefix : basePrefix + "/");
    return bp + tenantId + "/" + application + "/" + hourBucket + "/";
  }

  @Override
  public List<String> keyFor(String tenantId, String application, long hourBucket) {
    // Return the hour-level prefix; callers will list under it
    return java.util.List.of(basePath(tenantId, application, hourBucket));
  }

  @Override
  public String uploadKey(String tenantId, String application, long hourBucket, String nodeId) {
    return basePath(tenantId, application, hourBucket)
        + (nodeId == null ? "unknown" : nodeId)
        + "/tracefile.bin";
  }
}
