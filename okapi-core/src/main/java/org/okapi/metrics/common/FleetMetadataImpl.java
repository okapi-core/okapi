package org.okapi.metrics.common;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorOp;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger;
import org.apache.zookeeper.KeeperException;

public class FleetMetadataImpl implements FleetMetadata {
  CuratorFramework curatorFramework;
  Map<String, DistributedAtomicInteger> counters;
  RetryPolicy retryPolicy;
  ReadWriteLock counterMapRwLock;

  public FleetMetadataImpl(CuratorFramework curatorFramework, RetryPolicy retryPolicy) {
    checkNotNull(curatorFramework);
    checkNotNull(retryPolicy);
    this.curatorFramework = curatorFramework;
    this.retryPolicy = retryPolicy;
    this.counters = new HashMap<>();
    this.counterMapRwLock = new ReentrantReadWriteLock();
  }

  @Override
  public byte[] getData(String path) throws Exception {
    try {
      return curatorFramework.getData().forPath(path);
    } catch (KeeperException.NoNodeException e) {
      return new byte[] {};
    } catch (Exception e) {
      throw e;
    }
  }

  @Override
  public void create(String path) throws Exception {
    curatorFramework.create().creatingParentsIfNeeded().forPath(path);
  }

  @Override
  public void createParentsIfNeeded(String path) throws Exception {
    var exists = curatorFramework.checkExists().forPath(path);
    if (exists != null) {
      return;
    }
    curatorFramework.create().creatingParentsIfNeeded().forPath(path);
  }

  @Override
  public void setData(String path, byte[] data) throws Exception {
    createPathIfNotExists(path);
    curatorFramework.setData().forPath(path, data);
  }

  @Override
  public void incCounter(String path) throws Exception {
    populateCounter(path);
    counters.get(path).increment();
  }

  @Override
  public void decCounter(String path) throws Exception {
    populateCounter(path);
    var counter = counters.get(path);
    var count = counter.get();
    if (count.succeeded()) {
      var oldValue = count.postValue();
      if (oldValue > 0) {
        counter.decrement();
      }
    }
  }

  @Override
  public int getCounter(String path) throws Exception {
    var counter = counters.get(path);
    var count = counter.get();
    if (count.succeeded()) {
      return count.postValue();
    } else {
      throw new Exception("Failed to get counter for path: " + path);
    }
  }

  @Override
  public boolean atomicWrite(List<String> paths, List<byte[]> data) throws Exception {
    var txns = new ArrayList<CuratorOp>();
    for (var path : paths) {
      createPathIfNotExists(path);
    }
    for (int i = 0; i < paths.size(); i++) {
      txns.add(curatorFramework.transactionOp().setData().forPath(paths.get(i), data.get(i)));
    }
    var results = curatorFramework.transaction().forOperations(txns);
    var succeed = Iterables.all(results, r -> r.getError() == 0);
    if (!succeed) {
      throw new Exception("Atomic write failed.");
    }
    return true;
  }

  @Override
  public void createPathIfNotExists(String path) throws Exception {
    var checkExists = curatorFramework.checkExists().forPath(path);
    if (checkExists != null) {
      return;
    }
    create(path);
  }

  @Override
  public List<String> listChildren(String path) throws Exception {
    var children = curatorFramework.getChildren().forPath(path);
    return children;
  }

  public void populateCounter(String path){
    if(counters.containsKey(path)) return;
    counterMapRwLock.writeLock().lock();
    try {
      if(counters.containsKey(path)) return;
      var counter = new DistributedAtomicInteger(curatorFramework, path, retryPolicy);
      counters.put(path, counter);
    }
    finally{
      counterMapRwLock.writeLock().unlock();
    }
  }
}
