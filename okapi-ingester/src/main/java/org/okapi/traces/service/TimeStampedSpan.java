package org.okapi.traces.service;

import io.opentelemetry.proto.trace.v1.Span;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.abstractingester.NanoTimeStampedRecord;

@AllArgsConstructor
@Getter
public class TimeStampedSpan implements NanoTimeStampedRecord {
  Span span;

  @Override
  public long getTimeUnixNano() {
    return span.getStartTimeUnixNano();
  }
}
