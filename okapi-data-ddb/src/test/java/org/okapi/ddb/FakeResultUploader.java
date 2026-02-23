/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.ddb;

import java.util.Map;
import org.okapi.data.dao.ResultUploader;

public class FakeResultUploader implements ResultUploader {
  Map<String, String> storedResults = new java.util.HashMap<>();

  @Override
  public String uploadResult(String orgId, String jobId, String resultData) {
    var key = orgId + "/" + jobId + "/result";
    storedResults.put(key, resultData);
    return key;
  }

  @Override
  public String getRawResult(String orgId, String jobId) {
    var key = orgId + "/" + jobId + "/result";
    return storedResults.get(key);
  }
}
