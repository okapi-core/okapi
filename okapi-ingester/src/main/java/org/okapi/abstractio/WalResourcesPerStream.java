/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.abstractio;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Locked;
import org.okapi.wal.LsnSupplier;
import org.okapi.wal.factory.WalResourceBundle;
import org.okapi.wal.io.WalReader;
import org.okapi.wal.io.WalWriter;
import org.okapi.wal.manager.WalManager;

public class WalResourcesPerStream<Id> {

  Map<Id, WalResourceBundle> resourceBundles;
  ReadWriteLock writeLock = new ReentrantReadWriteLock();

  @Locked.Write("writeLock")
  public void addBundle(Id streamId, WalResourceBundle resourceBundle) {
    resourceBundles.put(streamId, resourceBundle);
  }

  public WalResourcesPerStream() {
    this.resourceBundles = new HashMap<>();
  }

  private WalResourceBundle getResourceBundle(Id sh) {
    var res = this.resourceBundles.get(sh);
    Preconditions.checkNotNull(res, "No resource bundle found for :" + sh);
    return res;
  }

  public WalManager getWalManager(Id shard) {
    return getResourceBundle(shard).getManager();
  }

  public WalWriter getWalWriter(Id shard) {
    return getResourceBundle(shard).getWriter();
  }

  public WalReader getWalReader(Id shard) {
    return getResourceBundle(shard).getReader();
  }

  public LsnSupplier getLsnSupplier(Id shard) {
    return getResourceBundle(shard).getLsnSupplier();
  }
}
