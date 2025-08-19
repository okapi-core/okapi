package org.okapi.metrics.sharding.fakes;

import java.util.*;
import lombok.Setter;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssignerFactory;

public class FixedAssignerFactory implements ShardsAndSeriesAssignerFactory {

  @Setter
  Map<String, ShardsAndSeriesAssigner> assignerMap = new HashMap<>();

  @Override
  public ShardsAndSeriesAssigner makeAssigner(int nShards, List<String> nodes) {
    var key = hashKey(nShards, new TreeSet<>(nodes));
    var val = assignerMap.get(key);
    if(val == null){
      throw new IllegalStateException("Encountered unknown hash value " + key);
    }
    else return val;
  }

  public String hashKey(int nShards, TreeSet<String> nodes){
    var sb = new StringBuilder();
    sb.append("shards=").append(nShards);
    for(var n : nodes){
      sb.append("node=" + n + ",") ;
    }
    return sb.toString();
  }

  public FixedAssignerFactory set(int nShards, TreeSet<String> nodes, ShardsAndSeriesAssigner assigner){
    var key = hashKey(nShards, nodes);
    assignerMap.put(key, assigner);
    return this;
  }
}
