package org.okapi.runtime.spans;

import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractio.BinFilesPrefixRegistry;
import org.okapi.abstractio.PartNames;
import org.okapi.runtime.AbstractS3Uploader;
import org.okapi.traces.config.TracesCfg;
import org.okapi.traces.paths.TracesDiskPaths;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
public class TraceFilesS3Uploader extends AbstractS3Uploader<String> {

  public TraceFilesS3Uploader(
      TracesCfg tracesCfg,
      @Autowired S3Client s3Client,
      TracesDiskPaths traceBinPaths,
      @Autowired BinFilesPrefixRegistry binFilesPrefixRegistry) {
    super(
        tracesCfg.getS3Bucket(),
        tracesCfg.getS3BasePrefix(),
        tracesCfg.getIdxExpiryDuration(),
        s3Client,
        traceBinPaths,
        binFilesPrefixRegistry,
        PartNames.SPAN_FILE_PART);
  }
}
