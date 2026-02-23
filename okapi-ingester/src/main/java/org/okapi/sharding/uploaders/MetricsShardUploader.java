package org.okapi.sharding.uploaders;

import org.okapi.runtime.metrics.MetricFilesS3Uploader;
import org.okapi.sharding.HashingShardAssigner;
import org.okapi.spring.configs.Profiles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class MetricsShardUploader extends AbstractBinPathsShardUploader {
  public MetricsShardUploader(
      HashingShardAssigner shardAssigner, MetricFilesS3Uploader metricFilesS3Uploader) {
    super(shardAssigner, metricFilesS3Uploader);
  }
}
