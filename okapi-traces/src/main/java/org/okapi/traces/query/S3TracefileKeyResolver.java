package org.okapi.traces.query;

public interface S3TracefileKeyResolver {
  String bucket();

  String keyFor(String tenantId, String application, long hourBucket);
}
