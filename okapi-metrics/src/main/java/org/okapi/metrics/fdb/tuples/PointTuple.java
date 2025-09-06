package org.okapi.metrics.fdb.tuples;

import com.apple.foundationdb.tuple.Tuple;
import lombok.AllArgsConstructor;
import org.okapi.metrics.fdb.SubspaceFactory;

@AllArgsConstructor
public class PointTuple {
  String universalPath;
  Long bucket;
  BUCKET_TYPE type;

  public Tuple toTuple() {
    return Tuple.from(universalPath, type, bucket);
  }

  public byte[][] pointQuery() {
    var subspace = SubspaceFactory.getMetricsSub();
    return new byte[][] {
      subspace.pack(Tuple.from(universalPath, type, bucket)), 
            subspace.pack(Tuple.from(universalPath, type, bucket + 1))
    };
  }

  public byte[][] inclusiveRange(long endBucket) {
    var subspace = SubspaceFactory.getMetricsSub();
    return new byte[][] {
      subspace.pack(Tuple.from(universalPath, type.name(), bucket)), subspace.pack(Tuple.from(universalPath, type.name(), endBucket + 1))
    };
  }
}
