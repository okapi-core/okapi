/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.abstractio;

import java.io.IOException;
import org.okapi.wal.lsn.Lsn;

public class WalCommitter<Id> {
  WalResourcesPerStream<Id> walResourcesPerStream;

  public void commitLsn(Id streamId, Lsn lsn) throws IOException {
    var manager = walResourcesPerStream.getWalManager(streamId);
    manager.commitLsn(lsn);
  }
}
