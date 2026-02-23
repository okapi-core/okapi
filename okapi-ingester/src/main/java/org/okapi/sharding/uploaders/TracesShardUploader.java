package org.okapi.sharding.uploaders;

import org.okapi.runtime.spans.TraceFilesS3Uploader;
import org.okapi.sharding.HashingShardAssigner;
import org.okapi.spring.configs.Profiles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class TracesShardUploader extends AbstractBinPathsShardUploader {
  public TracesShardUploader(
          HashingShardAssigner shardAssigner, TraceFilesS3Uploader traceFilesS3Uploader) {
    super(shardAssigner, traceFilesS3Uploader);
  }
}
