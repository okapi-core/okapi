package org.okapi.metrics.query.promql;

import java.util.HashMap;
import java.util.Map;

public class TsData {
  long[] ts;
  float[] vals;
  Map<Long, Float> valsAtTime;

  public TsData(long[] ts, float[] vals) {
    this.ts = ts;
    this.vals = vals;
    valsAtTime = new HashMap<>();
    for (int i = 0; i < ts.length; i++) {
      valsAtTime.put(ts[i], vals[i]);
    }
  }

  public float getValue(long t) {
    return valsAtTime.get(t);
  }
}
