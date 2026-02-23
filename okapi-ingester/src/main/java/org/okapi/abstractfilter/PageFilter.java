/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.abstractfilter;

import java.util.List;
import org.okapi.byterange.RangeIterationException;

public interface PageFilter<R, M> {
  Kind kind();

  boolean shouldReadPage(M pageMeta) throws RangeIterationException;

  default List<R> getMatchingRecords(List<R> record) {
    return record.stream().filter(this::matchesRecord).toList();
  }

  boolean matchesRecord(R record);

  enum Kind {
    REGEX,
    TRACE,
    LEVEL,
    SPAN,
    SPAN_ATTRIBUTE,
    AND,
    OR,
    MOCK
  }
}
