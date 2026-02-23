package org.okapi.runtime.metrics;

import org.okapi.abstractio.BinFilesPrefixRegistry;
import org.okapi.abstractio.PartNames;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.metrics.paths.MetricsDiskPaths;
import org.okapi.runtime.AbstractS3Uploader;
import org.okapi.spring.configs.Profiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@Component
public class MetricFilesS3Uploader extends AbstractS3Uploader<String> {

  public MetricFilesS3Uploader(
      MetricsCfg metricsCfg,
      @Autowired S3Client s3Client,
      MetricsDiskPaths metricsDiskPaths,
      @Autowired BinFilesPrefixRegistry binFilesPrefixRegistry) {
    super(
        metricsCfg.getS3Bucket(),
        metricsCfg.getS3BasePrefix(),
        metricsCfg.getIdxExpiryDuration(),
        s3Client,
        metricsDiskPaths,
        binFilesPrefixRegistry,
        PartNames.METRICS_FILE_PART);
  }
}
