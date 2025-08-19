package org.okapi.metricsproxy.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.MetadataFields;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.s3.S3Prefixes;
import org.okapi.metrics.scanning.EmptyFileException;
import org.okapi.metrics.scanning.HourlyCheckpointScanner;
import org.okapi.s3.S3ByteRangeCache;
import org.okapi.s3.S3Enhanced;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

@Slf4j
@AllArgsConstructor
public class MetadataCache {

  String databucket;
  S3Enhanced s3Enhanced;
  S3ByteRangeCache rangeCache;
  S3Client amazonS3;

  public Optional<String> getPrefix(String path, long epochHour, String tenantId)
      throws StreamReadingException, IOException, ExecutionException {
    var prefix = S3Prefixes.hourlyPrefixRoot(epochHour, tenantId);
    var children = s3Enhanced.listChildren(databucket, prefix);
    var queryScanner = new HourlyCheckpointScanner();
    var candidates = new ArrayList<String>();
    for (var child : children) {
      var s3Scanner = new S3ByteRangeScanner(databucket, child, amazonS3, rangeCache);
      try {
        var metadata = queryScanner.getMd(s3Scanner);
        if (metadata.containsKey(path)) {
          candidates.add(child);
        }
      } catch (EmptyFileException e) {
        log.error("Got empty file where not expected for file {}", child);
      }
    }

    if (candidates.isEmpty()) {
      return Optional.empty();
    }

    var mostRecent = -1L;
    var toPick = candidates.getFirst();
    for (var c : candidates) {
      var updateTime =
          Long.parseLong(
              amazonS3
                  .headObject(HeadObjectRequest.builder().bucket(databucket).key(c).build())
                  .metadata()
                  .get(MetadataFields.HOURLY_UPLOAD_TIME) // keys are lowercase in S3 metadata map
              );
      if (updateTime > mostRecent) {
        toPick = c;
        mostRecent = updateTime;
      }
    }
    return Optional.of(toPick);
  }

  public List<String> listPaths(long epochHour, String tenantId)
      throws StreamReadingException, IOException, ExecutionException {
    var prefix = S3Prefixes.hourlyPrefixRoot(epochHour, tenantId);
    var children = s3Enhanced.listChildren(databucket, prefix);
    var queryScanner = new HourlyCheckpointScanner();
    var paths = new ArrayList<String>();
    for (var child : children) {
      var s3Scanner = new S3ByteRangeScanner(databucket, child, amazonS3, rangeCache);
      var metrics = queryScanner.listMetrics(s3Scanner);
      paths.addAll(metrics);
    }
    return paths;
  }
}
