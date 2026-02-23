/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.io;

import java.util.List;
import org.okapi.pages.AbstractListBackedPageBody;
import org.okapi.primitives.BinarySpanRecordV2;

public class SpanPageBody extends AbstractListBackedPageBody<BinarySpanRecordV2> {

  public SpanPageBody() {
    super();
  }

  public SpanPageBody(List<BinarySpanRecordV2> spans) {
    super(spans);
  }

  public SpanPageBodySnapshot toSnapshot() {
    return new SpanPageBodySnapshot(snapshot());
  }
}
