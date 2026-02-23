/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.query;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.abstractio.LogStreamIdentifier;
import org.okapi.intervals.IntervalUtils;
import org.okapi.logs.query.QueryConfig;
import org.okapi.primitives.BinarySpanRecordV2;
import org.okapi.queryproc.TraceQueryProcessor;
import org.okapi.traces.TracesBufferPool;
import org.okapi.traces.io.SpanPageMetadata;

@Slf4j
@AllArgsConstructor
public class SpanBufferPoolQueryProcessor implements TraceQueryProcessor {
  private final TracesBufferPool pool;

  ///  todo: fixme - we need to query shards not streams
  @Override
  public List<BinarySpanRecordV2> getTraces(
      String app,
      long start,
      long end,
      PageFilter<BinarySpanRecordV2, SpanPageMetadata> filter,
      QueryConfig cfg) {
    var active = pool.snapshotActivePage(LogStreamIdentifier.of(app));
    var out = new ArrayList<BinarySpanRecordV2>();
    if (active != null
        && IntervalUtils.isOverlapping(start, end, active.getTsStart(), active.getTsEnd())) {
      out.addAll(filter.getMatchingRecords(active.getBodySnapshot().getSpans()));
    }
    // Include sealed pages that intersect the query range
    for (var sealed : pool.snapshotSealedPages(LogStreamIdentifier.of(app), start, end)) {
      if (IntervalUtils.isOverlapping(start, end, sealed.getTsStart(), sealed.getTsEnd())) {
        out.addAll(filter.getMatchingRecords(sealed.getBodySnapshot().getSpans()));
      }
    }
    return out;
  }
}
