/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.okapi.rest.promql.*;

public class SerializationTests {

  @Test
  public void testPromQlSerialization() {
    var result = new GetPromQlResponse<PromQlData<VectorSeries>>();
    var series = new VectorSeries();
    var now = System.currentTimeMillis() / 1000;
    var data = new PromQlData<VectorSeries>();
    data.setResultType(PromQlResultType.VECTOR);
    series.setMetric(Map.of("__name__", "new-metric", "service", "api"));
    series.setValue(new Sample(now, Float.toString(0.1f)));
    data.setResult(series);
    result.setData(data);
    result.setStatus("success");
    Gson gson = new GsonBuilder().registerTypeAdapter(Sample.class, new SampleAdapter()).create();
    var asJson = gson.toJson(result);
    System.out.println("asJson:" + asJson);
  }
}
