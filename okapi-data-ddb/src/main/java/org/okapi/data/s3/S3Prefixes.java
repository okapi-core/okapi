/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.s3;

public class S3Prefixes {

  public static String getDashboardDefinitionPrefix(String id) {
    return "dashboards/definitions/" + id + "/definitions.json";
  }
}
