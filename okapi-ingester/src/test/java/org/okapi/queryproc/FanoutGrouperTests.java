/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.queryproc;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.okapi.abstractio.LogStreamIdentifier;
import org.okapi.routing.StreamRouter;

public class FanoutGrouperTests {

  @Test
  void testFanoutGrouping_collapseAdjacent() {
    var identifier = new LogStreamIdentifier("s");
    var routingTable = new HashMap<Long, String>();
    routingTable.put(0L, "node1");
    routingTable.put(1L, "node1");
    routingTable.put(2L, "node2");
    var router = getRouter(routingTable);
    var grouper = new FanoutGrouper(router);
    var groups = grouper.getQueryBoundariesPerNode(identifier, 0, 2, 200L);
    Assertions.assertEquals(2, groups.size());
  }

  @Test
  void testFanoutGrouping_multipleNodesPerHour() {
    var identifier = new LogStreamIdentifier("s");
    var routingTable = new HashMap<Long, String>();
    routingTable.put(0L, "node1");
    routingTable.put(1L, "node1");
    var router = getRouter(routingTable);
    var grouper = new FanoutGrouper(router);
    var groups = grouper.getQueryBoundariesPerNode(identifier, 0, 2, 200L);
    Assertions.assertEquals(2, groups.size());
    Assertions.assertTrue(groups.containsKey("node1"));
    Assertions.assertArrayEquals(new long[] {0L, 400L}, groups.get("node1").get(0));
  }

  public StreamRouter getRouter(Map<Long, String> routingTable) {
    var router = mock(StreamRouter.class);
    for (var entry : routingTable.keySet()) {
      when(router.getNodesForReading(any(), eq(entry))).thenReturn(routingTable.get(entry));
    }
    return router;
  }
}
