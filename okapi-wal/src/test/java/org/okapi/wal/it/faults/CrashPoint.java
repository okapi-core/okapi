package org.okapi.wal.it.faults;

public enum CrashPoint {
  BEFORE_LSN,
  AFTER_LSN_BEFORE_CRC,
  AFTER_CRC_BEFORE_PAYLOAD,
  DURING_PAYLOAD
}
