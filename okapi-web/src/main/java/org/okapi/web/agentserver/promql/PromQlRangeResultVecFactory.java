/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.agentserver.promql;

import java.util.TreeMap;
import org.okapi.agent.dto.results.promql.PromQlRangeResult;
import org.okapi.web.tsvector.TimeVector;
import org.okapi.web.tsvector.TreeMapTimeVector;
import org.okapi.web.tsvector.factory.TimeVectorFactory;

public class PromQlRangeResultVecFactory implements TimeVectorFactory<PromQlRangeResult> {

  @Override
  public TimeVector createTimeVector(PromQlRangeResult base) {
    int size = base.getValues().size();
    var map = new TreeMap<Long, Float>();
    for (int i = 0; i < size; i += 1) {
      var ts = ((Number) base.getValues().get(i).get(0)).doubleValue();
      var value = Float.parseFloat((String) base.getValues().get(i).get(1));
      map.put((long) ts, value);
    }
    return new TreeMapTimeVector(map);
  }
}
