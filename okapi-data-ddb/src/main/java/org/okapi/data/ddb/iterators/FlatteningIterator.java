/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.iterators;

import com.google.common.base.Preconditions;
import java.util.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

public class FlatteningIterator<T> implements Iterator<T> {

  private final Iterator<Page<T>> pages;
  private Iterator<T> currentItems;

  public FlatteningIterator(Iterator<Page<T>> cursor) {
    this.pages = Preconditions.checkNotNull(cursor, "Page iterator cannot be null");
    this.currentItems = Collections.emptyIterator();
    advance();
  }

  private void advance() {
    while ((currentItems == null || !currentItems.hasNext()) && pages.hasNext()) {
      Page<T> p = pages.next();
      List<T> items = (p == null) ? null : p.items();
      currentItems = (items == null) ? Collections.emptyIterator() : items.iterator();
    }
  }

  @Override
  public boolean hasNext() {
    if (currentItems != null && currentItems.hasNext()) return true;
    advance();
    return currentItems != null && currentItems.hasNext();
  }

  @Override
  public T next() {
    if (!hasNext()) throw new NoSuchElementException();
    return currentItems.next();
  }
}
