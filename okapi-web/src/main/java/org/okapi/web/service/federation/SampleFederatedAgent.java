package org.okapi.web.service.federation;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import org.okapi.web.generator.InternetTrafficGenerator;
import org.okapi.web.tsvector.HashMapTimeMatrix;
import org.okapi.web.tsvector.TimeMatrix;
import org.okapi.web.tsvector.TimeVector;
import org.okapi.web.tsvector.TreeMapTimeVector;

public class SampleFederatedAgent implements FederatedAgent {
  @Override
  public TimeMatrix getTimeMatrix(AgentContext context, String query) throws Exception {
    var start = System.currentTimeMillis() - 3600_000; // 1 hour ago
    var end = System.currentTimeMillis();
    var map = new TreeMap<String, TimeVector>();
    map.put("sample-1/" + query, getTimeVector(start, end));
    map.put("sample-2/" + query, getTimeVector(start, end));
    return new HashMapTimeMatrix(map);
  }

  @Override
  public TimeVector getTimeVector(AgentContext context, String query) throws Exception {
    var start = System.currentTimeMillis() - 3600_000; // 1 hour ago
    var end = System.currentTimeMillis();
    return getTimeVector(start, end);
  }

  @Override
  public List<String> getList(AgentContext context, String query) throws Exception {
    return Arrays.asList("Choice - A", "Choice - B", "Choice - C");
  }

  public TimeVector getTimeVector(long start, long end) throws Exception {
    // generate mock samples from various distributions
    var samplingRate = 1000; // 1 sample per second
    var size = (end - start) / samplingRate;
    var generator = InternetTrafficGenerator.defaultHourly();
    var sample = generator.generate(start, (int) size);
    var map = new TreeMap<Long, Float>();
    for (int i = 0; i < sample.ts().size(); i++) {
      map.put(sample.ts().get(i), sample.values().get(i));
    }
    return new TreeMapTimeVector(map);
  }
}
