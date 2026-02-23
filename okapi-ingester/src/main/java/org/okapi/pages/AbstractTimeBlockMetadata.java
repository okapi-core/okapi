/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

import lombok.Getter;

public class AbstractTimeBlockMetadata {
  @Getter long maxLsn;
  @Getter long tsStart;
  @Getter long tsEnd;

  protected void setTsStart(long tsStart) {
    this.tsStart = tsStart;
  }

  protected void setTsEnd(long tsEnd) {
    this.tsEnd = tsEnd;
  }

  public void setMaxLsn(long lsn) {
    if (lsn < maxLsn) return;
    this.maxLsn = lsn;
  }

  public void updateTsStart(Long ts) {
    if (ts == null) {
      return;
    }
    if (tsStart == 0) {
      tsStart = ts;
    } else {
      tsStart = Math.min(tsStart, ts);
    }
  }

  public void updateTsEnd(Long ts) {
    if (ts == null) {
      return;
    }
    if (tsEnd == 0) {
      tsEnd = ts;
    } else {
      tsEnd = Math.max(tsEnd, ts);
    }
  }
}
