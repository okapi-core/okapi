package org.okapi.metricsproxy.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.okapi.clock.Clock;
import org.okapi.clock.SystemClock;
import org.okapi.metrics.*;
import org.okapi.metrics.common.FleetMetadata;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.rollup.FrozenMetricsUploader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ActiveProfiles("test")
public class MetricsProxyTestConfig {

  @Bean
  public CheckpointUploaderDownloader checkpointUploaderDownloader(
      @Autowired S3Client s3, @Value("${dataBucket}") String databucket, @Autowired Clock clock) {
    return new S3CheckpointUploaderDownloader(databucket, s3, clock);
  }

  @Bean
  public ShardMap shardMap() {
    return new ShardMap(new SystemClock());
  }

  @Bean
  public PathRegistry pathRegistry() throws IOException {
    Path shardMapCheckpointRoot = Files.createTempDirectory("shardMapCkpt");
    Path hourlyCheckpointRoot = Files.createTempDirectory("hourlyCheckpointRoot");
    Path parquetRoot = Files.createTempDirectory("parquetRoot");
    return new PathRegistryImpl(shardMapCheckpointRoot, hourlyCheckpointRoot, parquetRoot);
  }

  @Bean
  public NodeStateRegistry nodeStateRegistry(
      @Autowired FleetMetadata fleetMetadata, @Autowired Node node) {
    return new NodeStateRegistryImpl(fleetMetadata, node);
  }

  @Bean
  public FrozenMetricsUploader hourlyCheckpointWriter(
      @Autowired ShardMap shardMap,
      @Autowired CheckpointUploaderDownloader checkpointUploaderDownloader,
      @Autowired PathRegistry pathRegistry,
      @Autowired NodeStateRegistry nodeStateRegistry,
      @Value("${admissionWindowHrs}") long admissionWindow,
      @Autowired Clock clock) {
    return new FrozenMetricsUploader(
        shardMap,
        checkpointUploaderDownloader,
        pathRegistry,
        nodeStateRegistry,
        clock,
        admissionWindow);
  }
}
