package org.okapi.traces.query;

import java.util.List;

public interface S3TracefileKeyResolver {
  String bucket();

  // Returns list of hour-level prefixes to scan for objects under.
  // Example: [basePrefix/tenant/app/hourBucket/]
  List<String> keyFor(String tenantId, String application, long hourBucket);

  // Returns the full S3 object key to upload a tracefile for a specific nodeId.
  // Example: basePrefix/tenant/app/hourBucket/nodeId/tracefile.bin
  String uploadKey(String tenantId, String application, long hourBucket, String nodeId);
}
