package org.okapi.data.s3;

public class S3Prefixes {

  public static String getDashboardDefinitionPrefix(String id) {
    return "dashboards/definitions/" + id + "/definitions.json";
  }
}
