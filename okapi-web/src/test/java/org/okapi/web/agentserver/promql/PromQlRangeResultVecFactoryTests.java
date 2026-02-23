/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.agentserver.promql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.okapi.agent.dto.results.promql.PromQlRangeResult;

public class PromQlRangeResultVecFactoryTests {

  @Test
  void testCreateTimeVector() {
    PromQlRangeResultVecFactory promQlRangeResultVecFactory = new PromQlRangeResultVecFactory();
    PromQlRangeResult result = new PromQlRangeResult();
    var values = new ArrayList<List<Object>>();
    var sample1 =
        new ArrayList<Object>() {
          {
            add(1627849200.0);
            add("1.0");
          }
        };
    var sample2 =
        new ArrayList<Object>() {
          {
            add(1627849260.0);
            add("2.0");
          }
        };
    values.add(sample1);
    values.add(sample2);
    result.setValues(values);
    result.setMetric(Map.of("instance", "localhost:9090", "job", "prometheus"));
    var vector = promQlRangeResultVecFactory.createTimeVector(result);
    Assertions.assertEquals(List.of(1627849200L, 1627849260L), vector.timestamps());
    Assertions.assertEquals(List.of(1.0f, 2.0f), vector.values());
  }
}
