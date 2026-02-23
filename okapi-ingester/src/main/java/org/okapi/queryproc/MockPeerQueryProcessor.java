/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.queryproc;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.identity.MemberList;
import org.okapi.identity.WhoAmI;
import org.okapi.logs.query.QueryConfig;
import org.okapi.pages.MockPageInput;
import org.okapi.pages.MockPageMetadata;
import org.okapi.routing.StreamRouter;
import org.okapi.streams.StreamIdentifier;

public class MockPeerQueryProcessor
    extends AbstractPeerQueryProcessor<MockPageInput, MockPageMetadata, String> {
  ExecutorService executorService;
  @Getter List<QueryArgs> peerQueries = new ArrayList<>();
  Multimap<QueryArgs, MockPageInput> mockDataPerNode;

  public MockPeerQueryProcessor(StreamRouter streamRouter, MemberList memberList, WhoAmI whoAmI) {
    super(memberList, whoAmI, new FanoutGrouper(streamRouter));
    this.executorService = Executors.newFixedThreadPool(4);
    this.mockDataPerNode = ArrayListMultimap.create();
  }

  public void addMockData(String ip, int port, long start, long end, MockPageInput... pages) {
    var args = new QueryArgs(ip, port, start, end);
    mockDataPerNode.putAll(args, Arrays.asList(pages));
  }

  @Override
  public List<MockPageInput> queryPeer(
      String ip,
      int port,
      StreamIdentifier<String> streamIdentifier,
      long start,
      long end,
      PageFilter<MockPageInput, MockPageMetadata> filter,
      QueryConfig cfg) {
    var args = new QueryArgs(ip, port, start, end);
    peerQueries.add(args);
    if (!mockDataPerNode.containsKey(args)) {
      throw new RuntimeException("No mock data for node: " + args);
    }
    return mockDataPerNode.get(args).stream().toList();
  }

  @Override
  public ExecutorService getExecutorService() {
    return this.executorService;
  }

  public record QueryArgs(String ip, int port, long start, long end) {}
}
