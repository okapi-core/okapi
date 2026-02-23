/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.aws;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public class NoOpCredentials implements AwsCredentialsProvider {
  @Override
  public AwsCredentials resolveCredentials() {
    return new AwsCredentials() {
      @Override
      public String accessKeyId() {
        return "";
      }

      @Override
      public String secretAccessKey() {
        return "";
      }
    };
  }
}
