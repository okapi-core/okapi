/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.query;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.logs.io.LogPageMetadata;
import org.okapi.primitives.BinaryLogRecordV1;

@Slf4j
@Value
public class LevelPageFilter implements PageFilter<BinaryLogRecordV1, LogPageMetadata> {
  int levelCode;

  public int levelCode() {
    return levelCode;
  }

  @Override
  public Kind kind() {
    return Kind.LEVEL;
  }

  @Override
  public boolean shouldReadPage(LogPageMetadata pageMeta) {
    return pageMeta.maybeContainsLeveInPage(levelCode);
  }

  @Override
  public boolean matchesRecord(BinaryLogRecordV1 record) {
    return record.getLevel() == levelCode;
  }
}
