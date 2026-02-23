/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.primitives;

import java.util.ListIterator;

public class UnmodifiableListIterator<T> implements ListIterator<T> {
  T[] contents;
  int off;
  int len;

  public UnmodifiableListIterator(T[] contents, int off, int len) {
    this.contents = contents;
    this.off = off;
    this.len = len;
  }

  int st = off;

  @Override
  public boolean hasNext() {
    return st < off + len;
  }

  @Override
  public T next() {
    return contents[st++];
  }

  @Override
  public boolean hasPrevious() {
    return st > off;
  }

  @Override
  public T previous() {
    return contents[--st];
  }

  @Override
  public int nextIndex() {
    return (st - off) + 1;
  }

  @Override
  public int previousIndex() {
    return (st - off) - 1;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(T t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(T t) {
    throw new UnsupportedOperationException();
  }
}
