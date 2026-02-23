/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.okapi.abstractio.LogStreamIdentifier;
import org.okapi.wal.lsn.Lsn;

public class ActivePageTests {

  LogStreamIdentifier mockId = new LogStreamIdentifier("stream1");
  Lsn mockLsn = Lsn.fromNumber(100);
  MockPageInput mockPageInput = new MockPageInput(1, "data1", 1000);

  @Test
  void testAppendOnFullPage() {
    var ap = new ActivePage<>(mockId, (id) -> new MockAppendPage(10, 1000));
    var afterApply = ap.append(mockLsn, mockPageInput);
    assertTrue(afterApply.isPresent());
  }

  @Test
  void testAppendOnNonFullPage() {
    var ap = new ActivePage<>(mockId, (id) -> new MockAppendPage(1000, 10000));
    var afterApply = ap.append(mockLsn, mockPageInput);
    assertTrue(afterApply.isEmpty());
  }

  @Test
  void testRotateIfOlderThanAndNotEmpty() {
    var ap = new ActivePage<>(mockId, (id) -> new MockAppendPage(1000, 10000));
    ap.append(mockLsn, mockPageInput);
    var rotated = ap.rotateIfOlderThanAndNotEmpty(10);
    assertTrue(rotated.isPresent());
  }

  @Test
  void testRotateIfNonEmpty() {
    var ap = new ActivePage<>(mockId, (id) -> new MockAppendPage(1000, 10000));
    ap.append(mockLsn, mockPageInput);
    var rotated = ap.rotateIfNonEmpty();
    assertTrue(rotated.isPresent());
  }

  @Test
  void testRotateIfNonEmptyOnEmptyPage() {
    var ap = new ActivePage<>(mockId, (id) -> new MockAppendPage(1000, 10000));
    var rotated = ap.rotateIfNonEmpty();
    assertTrue(rotated.isEmpty());
  }

  @Test
  void testRotateIfOlderThan_AndNotEmpty_nonEmptyPage() {}

  @Test
  void testRotateIfOlderThan_AndNotEmpty_emptyPage() {
    var ap = new ActivePage<>(mockId, (id) -> new MockAppendPage(1000, 10000));
    var rotated = ap.rotateIfOlderThanAndNotEmpty(1000L);
    assertTrue(rotated.isEmpty());
  }
}
