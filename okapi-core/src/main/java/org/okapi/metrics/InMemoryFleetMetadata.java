package org.okapi.metrics;

import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.common.FleetMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class InMemoryFleetMetadata implements FleetMetadata {
  ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  Map<String, byte[]> dataStore = new HashMap<>();
  ReentrantLock counterLock = new ReentrantLock();
  Map<String, AtomicInteger> counters = new HashMap<>();

  @Override
  public byte[] getData(String path) {
    return dataStore.get(path);
  }

  @Override
  public void create(String path) {
    createParentsIfNeeded(path);
    setData(path, new byte[0]);
  }

  @Override
  public void createParentsIfNeeded(String path) {
    var parts = path.split("/");
    StringBuilder currentPath = new StringBuilder();

    for (int i = 1; i < parts.length; i++) {
      currentPath.append("/").append(parts[i]);
      if (!dataStore.containsKey(currentPath.toString())) {
        setData(currentPath.toString(), new byte[0]);
      }
    }
  }

  @Override
  public void setData(String path, byte[] data) {
    if(data == null){
      log.error("Setting data for path {} to null", path);
      throw new RuntimeException();
    }
    dataStore.put(path, data);
  }

  @Override
  public void incCounter(String path) throws Exception {
    counterLock.lock();
    counters.put(path, counters.getOrDefault(path, new AtomicInteger(0)));
    counterLock.unlock();
    counters.get(path).incrementAndGet();
  }

  @Override
  public void decCounter(String path) throws Exception {
    counterLock.lock();
    counters.put(path, counters.getOrDefault(path, new AtomicInteger(0)));
    counterLock.unlock();
    counters.get(path).decrementAndGet();
  }

  @Override
  public int getCounter(String path) throws Exception {
    counterLock.lock();
    counters.put(path, counters.getOrDefault(path, new AtomicInteger(0)));
    counterLock.unlock();
    return counters.get(path).get();
  }

  @Override
  public boolean atomicWrite(List<String> paths, List<byte[]> data) throws Exception {
    readWriteLock.writeLock().lock();
    try {
      for (int i = 0; i < paths.size(); i++) {
        this.setData(paths.get(i), data.get(i));
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
    return true;
  }

  @Override
  public void createPathIfNotExists(String path) throws Exception {
    readWriteLock.writeLock().lock();
    try {
      if (!dataStore.containsKey(path)) {
        create(path);
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public List<String> listChildren(String path) throws Exception {
    return dataStore.keySet().stream()
        .filter(p -> p.startsWith(path) && !p.equals(path))
        .map(p -> p.substring(path.length() + 1).split("/")[0])
        .distinct()
        .toList();
  }
}
