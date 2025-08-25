package org.okapi.metrics.common;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class FleetMetadataImplTest {

  FleetMetadataImpl f;
  Map<String, DistributedAtomicInteger> counters;
  CuratorFramework client;
  RetryPolicy retryPolicy;

  @BeforeEach
  public void setup() {
    counters = new HashMap<>();
    retryPolicy = new ExponentialBackoffRetry(100, 2);
    client =
            CuratorFrameworkFactory.newClient("localhost:2181", retryPolicy);
    client.start();
    f = new FleetMetadataImpl(client, retryPolicy);
  }

  @Test
  public void testWritePathWithoutExists() throws Exception {
     var uniquePath = "/path/to/id/" + UUID.randomUUID();
     f.createParentsIfNeeded(uniquePath);
     var exists = client.checkExists().forPath(uniquePath);
     assertNotNull(exists);
  }

  @Test
  public void testMultipleIfNeededCausesNoExceptions() throws Exception {
    var uniquePath = "/path/to/id/" + UUID.randomUUID();
    f.createParentsIfNeeded(uniquePath);
    f.createParentsIfNeeded(uniquePath);
    var exists = client.checkExists().forPath(uniquePath);
    assertNotNull(exists);
  }

  @Test
  public void testCounter() throws Exception {
    var counterPath = "/path/to/counter" + UUID.randomUUID();
    var counter = new DistributedAtomicInteger(client, counterPath, retryPolicy);
    counters.put(counterPath, counter);
    f.incCounter(counterPath);
    
    var count = counter.get();
    assertTrue(count.succeeded());
    assertEquals(1, count.postValue());
  }

  @Test
  public void testCounterMultipleInc() throws Exception {
    var counterPath = "/path/to/counter" + UUID.randomUUID();
    var counter = new DistributedAtomicInteger(client, counterPath, retryPolicy);
    counters.put(counterPath, counter);
    f.incCounter(counterPath);
    f.incCounter(counterPath);

    var count = counter.get();
    assertTrue(count.succeeded());
    assertEquals(2, count.postValue());
  }

  @Test
  public void testCounterIncDec() throws Exception {
    var counterPath = "/path/to/counter" + UUID.randomUUID();
    var counter = new DistributedAtomicInteger(client, counterPath, retryPolicy);
    counters.put(counterPath, counter);
    f.incCounter(counterPath);
    f.decCounter(counterPath);

    var count = counter.get();
    assertTrue(count.succeeded());
    assertEquals(0, count.postValue());
  }
}
