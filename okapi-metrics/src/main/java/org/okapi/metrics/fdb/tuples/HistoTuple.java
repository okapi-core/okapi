package org.okapi.metrics.fdb.tuples;

import com.apple.foundationdb.tuple.Tuple;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.metrics.fdb.SubspaceFactory;

@AllArgsConstructor
@Getter
public class HistoTuple implements FdbTuple {
  String universalPath;
  Float ub;
  boolean inf;
  long startBucket;
  long endBucket;
  String nodeId;

  @Override
  public byte[] pack() {
    var subspace = SubspaceFactory.getHistoSubspace();
    if (inf) {
      var tuple = Tuple.from(universalPath, startBucket, endBucket, "inf", nodeId);
      return subspace.pack(tuple);
    } else {
      var tuple = Tuple.from(universalPath, startBucket, endBucket, ub, nodeId);
      return subspace.pack(tuple);
    }
  }

  public static Tuple[] rangeScanHalfOpen(String universalPath, long startBucket, long endBucket) {
    var startTuple = Tuple.from(universalPath, startBucket);
    var endTuple = Tuple.from(universalPath, endBucket + 1);
    return new Tuple[] {startTuple, endTuple};
  }
}
