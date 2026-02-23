package org.okapi.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ZkClientTests {

  CuratorFramework curatorFramework;

  @BeforeEach
  void setup() throws Exception {
    var testingServer = new TestingServer();
    testingServer.start();
    curatorFramework =
        CuratorFrameworkFactory.builder()
            .connectString(testingServer.getConnectString())
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .build();
    curatorFramework.start();
  }

  @Test
  void testWrietNodeCas() throws Exception {
    var writer = new ZkClient(curatorFramework);
    var data = "data".getBytes();
    writer.writeNodeCas("/test-path/data", data, 0);
  }

  @Test
  void testWriteFailsWithoutCorrectVersion() throws Exception {
    var writer = new ZkClient(curatorFramework);
    var data = "data".getBytes();
    writer.writeNodeCas("/test-path/data", data, 0);
    try {
      writer.writeNodeCas("/test-path/data", data, 0);
      throw new RuntimeException("Expected exception not thrown");
    } catch (Exception e) {
      // expected
    }
  }

  @Test
  void testWriteSucceedsWithCorrectVersion() throws Exception {
    var writer = new ZkClient(curatorFramework);
    var data = "data".getBytes();
    var stat = writer.writeNodeCas("/test-path/data", data, 0);
    writer.writeNodeCas("/test-path/data", data, stat.getVersion());
  }

  @Test
  void testReadNode() throws Exception {
    var writer = new ZkClient(curatorFramework);
    var data = "data".getBytes();
    writer.writeNodeCas("/test-path/data", data, 0);
    var readData = writer.readNode("/test-path/data");
    assert new String(readData).equals("data");
  }

  @Test
  void testReadNodeWithStat() throws Exception {
    var writer = new ZkClient(curatorFramework);
    var data = "data".getBytes();
    writer.writeNodeCas("/test-path/data", data, 0);
    var stat = new org.apache.zookeeper.data.Stat();
    var readData = writer.readNode("/test-path/data", stat);
    assert new String(readData).equals("data");
    assert stat.getVersion() == 1;
  }

  @Test
  void testDeleteNode() throws Exception {
    var writer = new ZkClient(curatorFramework);
    var data = "data".getBytes();
    writer.writeNodeCas("/test-path/data", data, 0);
    try {
      writer.readNode("/test-path/data");
    } catch (Exception e) {
      throw new RuntimeException("Node should exist");
    }
    try {
      writer.deleteNode("/test-path/data");
    } catch (Exception e) {
      throw new RuntimeException("Failed to delete node");
    }
    try {
      writer.readNode("/test-path/data");
      throw new RuntimeException("Node should not exist");
    } catch (Exception e) {
      // expected
    }
  }

  @Test
  void testGetChildren() throws Exception {
    var writer = new ZkClient(curatorFramework);
    var data1 = "data1".getBytes();
    var data2 = "data2".getBytes();
    writer.writeNodeCas("/test-path/child1", data1, 0);
    writer.writeNodeCas("/test-path/child2", data2, 0);
    var children = writer.getChildren("/test-path");
    assert children.size() == 2;
    assert children.contains("child1");
    assert children.contains("child2");
  }

  @Test
  void testDelNonExistentNode() throws Exception {
    var writer = new ZkClient(curatorFramework);
    try {
      writer.deleteNode("/non-existent-path");
      throw new RuntimeException("Expected exception not thrown");
    } catch (Exception e) {
      // expected
    }
  }

  @Test
  void testListWithoutChildren() throws Exception {
    var writer = new ZkClient(curatorFramework);
    var children = writer.getChildren("/empty-path");
    assert children.isEmpty();
  }
}
