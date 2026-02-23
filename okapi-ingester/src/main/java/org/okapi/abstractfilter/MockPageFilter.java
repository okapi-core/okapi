package org.okapi.abstractfilter;

import org.okapi.byterange.RangeIterationException;

public class MockPageFilter<R, M> implements PageFilter<R, M> {
  @Override
  public Kind kind() {
    return Kind.MOCK;
  }

  @Override
  public boolean shouldReadPage(M pageMeta) throws RangeIterationException {
    return true;
  }

  @Override
  public boolean matchesRecord(R record) {
    return true;
  }
}
