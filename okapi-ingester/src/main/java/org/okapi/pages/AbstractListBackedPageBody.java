/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbstractListBackedPageBody<T> implements Snapshottable<T> {
  private final List<T> docs;

  public AbstractListBackedPageBody() {
    this.docs = new ArrayList<>();
  }

  protected AbstractListBackedPageBody(List<T> docs) {
    this.docs = docs;
  }

  public void add(T doc) {
    this.docs.add(doc);
  }

  @Override
  public List<T> snapshot() {
    return Collections.unmodifiableList(docs.subList(0, docs.size()));
  }

  public int size() {
    return docs.size();
  }
}
