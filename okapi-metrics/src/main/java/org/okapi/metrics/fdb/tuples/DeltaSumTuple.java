package org.okapi.metrics.fdb.tuples;

import com.apple.foundationdb.tuple.Tuple;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.metrics.fdb.SubspaceFactory;

@AllArgsConstructor
@Getter
public class DeltaSumTuple implements FdbTuple {

  String universalPath;
  String nodeId;
  long startBucket;
  long endBucket;

  @Override
  public byte[] pack() {
    var sumSubspace = SubspaceFactory.getSumSubspace();
    var tuple = Tuple.from(universalPath, startBucket, endBucket, nodeId);
    return sumSubspace.pack(tuple);
  }

  public static Tuple[] rangeScanHalfOpen(String universalPath, long startBucket, long endBucket) {
    var startTuple = Tuple.from(universalPath, startBucket);
    var endTuple = Tuple.from(universalPath, endBucket + 1);
    return new Tuple[] {startTuple, endTuple};
  }
}
