package org.okapi.sharding.uploaders;

import org.okapi.runtime.logs.LogsFilesS3Uploader;
import org.okapi.sharding.HashingShardAssigner;
import org.okapi.spring.configs.Profiles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@Service
public class LogsShardUploader extends AbstractBinPathsShardUploader {
  public LogsShardUploader(HashingShardAssigner shardAssigner, LogsFilesS3Uploader logsFilesS3Uploader) {
    super(shardAssigner, logsFilesS3Uploader);
  }
}
