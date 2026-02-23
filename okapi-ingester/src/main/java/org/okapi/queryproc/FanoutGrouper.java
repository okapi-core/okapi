package org.okapi.queryproc;

import static org.okapi.intervals.IntervalUtils.merge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.okapi.routing.StreamRouter;
import org.okapi.streams.StreamIdentifier;

@AllArgsConstructor
public class FanoutGrouper {
  StreamRouter streamRouter;

  public Map<String, List<long[]>> getQueryBoundariesPerNode(
      StreamIdentifier streamIdentifier, long blkStart, long blkEnd, long idxDuration) {
    Map<String, List<long[]>> queryHrBoundaryPerMember = new HashMap<>();
    for (long hr = blkStart; hr <= blkEnd; hr++) {
      long hourStartMillis = hr * idxDuration;
      long hourEndMillis = hourStartMillis + idxDuration;
      var node = streamRouter.getNodesForReading(streamIdentifier, hr);
      queryHrBoundaryPerMember
          .computeIfAbsent(node, (n) -> new ArrayList<>())
          .add(new long[] {hourStartMillis, hourEndMillis});
    }
    var keySet = queryHrBoundaryPerMember.keySet();
    for (var k : keySet) {
      var merged = merge(queryHrBoundaryPerMember.get(k));
      queryHrBoundaryPerMember.put(k, merged);
    }
    return queryHrBoundaryPerMember;
  }
}
