/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.spring.configs;

import org.okapi.abstractio.BinFilesPrefixRegistry;
import org.okapi.abstractio.NodeIdSeparatedBinFilesPrefixRegistry;
import org.okapi.nodes.NodeIdSupplier;
import org.okapi.s3.S3ByteRangeCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3Client;

@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@Configuration
public class S3Configuration {

  @Bean
  public S3ByteRangeCache s3ByteRangeCache(
      @Value("${okapi.logs.s3.cache.max-size-bytes:104857600}") long maxSizeBytes) {
    return new S3ByteRangeCache(maxSizeBytes);
  }

  @Bean
  public BinFilesPrefixRegistry s3LogsPrefixRegistry(
      @Autowired NodeIdSupplier nodeIdSupplier, @Autowired S3Client s3Client) {
    return new NodeIdSeparatedBinFilesPrefixRegistry(nodeIdSupplier.getNodeId(), s3Client);
  }
}
