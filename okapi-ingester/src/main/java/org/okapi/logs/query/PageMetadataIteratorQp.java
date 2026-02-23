/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.byterange.RangeIterationException;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;
import org.okapi.pages.AppendOnlyPage;
import org.okapi.pages.Codec;
import org.okapi.pages.PageAndMetadataIterator;
import org.okapi.pages.Snapshottable;

@Slf4j
@AllArgsConstructor
public class PageMetadataIteratorQp<
    Page extends AppendOnlyPage<?, Snap, Metadata, Body>,
    Snap,
    Metadata,
    Record,
    Body extends Snapshottable<Record>> {

  PageAndMetadataIterator metadataIterator;
  PageFilter<Record, Metadata> filter;
  Codec<Page, Snap, Metadata, Body> codec;

  public List<Record> getMatchingRecords()
      throws IOException, StreamReadingException, NotEnoughBytesException, RangeIterationException {
    var aggregated = new ArrayList<Record>();
    while (metadataIterator.hasNextPage()) {
      var metadata = metadataIterator.readMetadata();
      var parsed = codec.deserializeMetadata(metadata);
      if (parsed.isPresent() && filter.shouldReadPage(parsed.get().metadata())) {
        var page = metadataIterator.readPageBody();
        var parsedBody = codec.deserializeBody(page, 0, page.length);
        parsedBody.ifPresent(
            pageBody -> aggregated.addAll(filter.getMatchingRecords(pageBody.snapshot())));
      }
      metadataIterator.forward();
    }
    return aggregated;
  }
}
