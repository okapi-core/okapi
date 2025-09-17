package org.okapi.metrics.fdb;

import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@Value
public class FdbTx {
  byte[] key;
  byte[] val;

  public int size() {
    return this.key.length + this.val.length;
  }
}
