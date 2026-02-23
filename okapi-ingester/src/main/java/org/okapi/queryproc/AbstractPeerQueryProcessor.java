/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.queryproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import lombok.AllArgsConstructor;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.identity.MemberList;
import org.okapi.identity.WhoAmI;
import org.okapi.logs.query.QueryConfig;
import org.okapi.streams.StreamIdentifier;

@AllArgsConstructor
public abstract class AbstractPeerQueryProcessor<R, M, Id> {

  MemberList memberList;
  WhoAmI whoAmI;
  FanoutGrouper fanoutGrouper;

  public List<R> getPeerResults(
      StreamIdentifier<Id> streamIdentifier,
      long start,
      long end,
      long idxDuration,
      PageFilter<R, M> filter,
      QueryConfig cfg)
      throws IOException {
    long hrStart = start / idxDuration;
    long hrEnd = (end - 1) / idxDuration;

    // Member -> list of [start,end) hour ranges (all members in the block, skip self)
    Map<String, List<long[]>> queryHrBoundaryPerMember =
        fanoutGrouper.getQueryBoundariesPerNode(streamIdentifier, hrStart, hrEnd, idxDuration);

    // Merge adjacent hour ranges per member and fan-out in parallel
    List<CompletableFuture<List<R>>> futures = new ArrayList<>();
    for (var entry : queryHrBoundaryPerMember.entrySet()) {
      var nodeId = entry.getKey();
      if (nodeId.equals(whoAmI.getNodeId())) continue;
      var member = memberList.getMember(nodeId);

      for (long[] m : entry.getValue()) {
        long qStart = Math.max(start, m[0]);
        long qEnd = Math.min(end, m[1]);
        if (qStart > qEnd) {
          throw new RuntimeException(
              String.format("The query configuration is invalid: %d %d", qStart, qEnd));
        }
        futures.add(
            CompletableFuture.supplyAsync(
                () ->
                    this.queryPeer(
                        member.getIp(),
                        member.getPort(),
                        streamIdentifier,
                        qStart,
                        qEnd,
                        filter,
                        cfg),
                getExecutorService()));
      }
    }

    List<R> out = new ArrayList<>();
    for (var f : futures) {
      try {
        out.addAll(f.join());
      } catch (CompletionException ce) {
        Throwable cause = ce.getCause() != null ? ce.getCause() : ce;
        if (cause instanceof RuntimeException re) throw re;
        if (cause instanceof Error err) throw err;
        throw new IOException("Member fan-out failed", cause);
      }
    }
    return out;
  }

  public abstract List<R> queryPeer(
      String ip,
      int port,
      StreamIdentifier<Id> streamIdentifier,
      long start,
      long end,
      PageFilter<R, M> filter,
      QueryConfig cfg);

  public abstract ExecutorService getExecutorService();
}
