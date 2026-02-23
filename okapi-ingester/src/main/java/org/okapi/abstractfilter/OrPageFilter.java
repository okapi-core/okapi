/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.abstractfilter;

import lombok.Value;
import org.okapi.byterange.RangeIterationException;

@Value
public class OrPageFilter<R, M> implements PageFilter<R, M> {
  PageFilter<R, M> left;
  PageFilter<R, M> right;

  @Override
  public Kind kind() {
    return Kind.OR;
  }

  @Override
  public boolean shouldReadPage(M pageMeta) throws RangeIterationException {
    return left.shouldReadPage(pageMeta) || right.shouldReadPage(pageMeta);
  }

  @Override
  public boolean matchesRecord(R record) {
    return left.matchesRecord(record) || right.matchesRecord(record);
  }
}
