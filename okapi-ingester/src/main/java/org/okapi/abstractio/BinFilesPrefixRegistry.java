/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.abstractio;

import java.util.List;

public interface BinFilesPrefixRegistry {
  String getRootPrefixForLogBinFile(String base, String streamId, long hrBlock);

  String getRootPrefixForLogBinFileForMe(
      String bucket, String base, String streamId, String partName, long hrBlock);

  List<String> getAllPrefixesForLogBinFile(
      String bucket, String base, String streamId, String partName, long hrBlock);
}
