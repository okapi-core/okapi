package org.okapi.metrics;

import com.google.common.primitives.Longs;
import org.okapi.metrics.common.FleetMetadata;
import org.okapi.metrics.common.ZkPaths;
import org.okapi.metrics.common.pojo.Node;
import java.util.Optional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NodeStateRegistryImpl implements NodeStateRegistry {
  FleetMetadata fleetMetadata;
  Node node;

  @Override
  public void updateLastCheckPointedHour(long hr) throws Exception {
    var id = node.id();
    var zkPath = ZkPaths.lastCheckpointedHour(id);
    fleetMetadata.createParentsIfNeeded(zkPath);
    fleetMetadata.setData(zkPath, Longs.toByteArray(hr));
  }

  @Override
  public Optional<Long> getLastCheckpointedHour() throws Exception {
    var id = node.id();
    var zkPath = ZkPaths.lastCheckpointedHour(id);
    var data = fleetMetadata.getData(zkPath);
    if(data == null || data.length == 0){
      return Optional.empty();
    }
    else {
      var parsed = Longs.fromByteArray(data);
      return Optional.of(parsed);
    }
  }
}
