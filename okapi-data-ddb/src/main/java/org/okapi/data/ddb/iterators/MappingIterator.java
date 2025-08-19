package org.okapi.data.ddb.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class MappingIterator<S, T> implements Iterator<T> {
  private final Iterator<S> delegate;
  private final Function<S, T> mapper;

  public MappingIterator(Iterator<S> delegate, Function<S, T> mapper) {
    this.delegate = delegate;
    this.mapper = mapper;
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public T next() {
    if (!delegate.hasNext()) throw new NoSuchElementException();
    return mapper.apply(delegate.next());
  }
}
