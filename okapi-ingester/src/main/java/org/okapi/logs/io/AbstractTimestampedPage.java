package org.okapi.logs.io;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.okapi.pages.AbstractTimeBlockMetadata;
import org.okapi.pages.InclusiveRange;

@AllArgsConstructor
public abstract class AbstractTimestampedPage {

  public Optional<InclusiveRange> range() {
    return Optional.of(
        new InclusiveRange(getBlockMetadata().getTsStart(), getBlockMetadata().getTsEnd()));
  }

  public long getTsStart() {
    return getBlockMetadata().getTsStart();
  }

  public long getTsEnd() {
    return getBlockMetadata().getTsEnd();
  }

  public boolean isTimeRangeFull(long expectedRangeMs){
    long actualRange = getTsEnd() - getTsStart();
    return actualRange >= expectedRangeMs;
  }

  public abstract AbstractTimeBlockMetadata getBlockMetadata();
}
