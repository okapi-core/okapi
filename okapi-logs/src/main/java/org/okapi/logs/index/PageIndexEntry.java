package org.okapi.logs.index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageIndexEntry {
  private long offset;
  private int length;
  private long tsStart;
  private long tsEnd;
  private int docCount;
  private int crc32;
}

