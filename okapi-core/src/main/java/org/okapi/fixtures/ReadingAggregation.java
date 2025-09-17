package org.okapi.fixtures;

import java.util.List;
import java.util.TreeMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ReadingAggregation {
  GaugeGenerator generator;
  List<Long> timestamp;
  List<Float> values;

  public TreeMap<Long, Float> asMap() {
    var tree = new TreeMap<Long, Float>();
    for (int i = 0; i < timestamp.size(); i++) {
      tree.put(timestamp.get(i), values.get(i));
    }
    return tree;
  }
}
