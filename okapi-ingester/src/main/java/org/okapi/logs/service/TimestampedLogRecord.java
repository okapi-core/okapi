package org.okapi.logs.service;

import io.opentelemetry.proto.logs.v1.LogRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.abstractingester.NanoTimeStampedRecord;

@AllArgsConstructor
@Getter
public class TimestampedLogRecord implements NanoTimeStampedRecord {
  LogRecord logRecord;

  @Override
  public long getTimeUnixNano() {
    return logRecord.getTimeUnixNano();
  }
}
