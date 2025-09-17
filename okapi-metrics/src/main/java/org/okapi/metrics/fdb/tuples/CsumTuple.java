package org.okapi.metrics.fdb.tuples;

import com.apple.foundationdb.tuple.Tuple;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.okapi.metrics.fdb.SubspaceFactory;

@AllArgsConstructor
@Value
public class CsumTuple implements FdbTuple {
  String universalPath;
  long startBucket;
  long endBucket;

  @Override
  public byte[] pack() {
    var sumSubspace = SubspaceFactory.getCsumSubspace();
    var tuple = Tuple.from(universalPath, startBucket, endBucket);
    return sumSubspace.pack(tuple);
  }



  public static Tuple[] rangeScanHalfOpen(String universalPath, long startBucket, long endBucket) {
    var startTuple = Tuple.from(universalPath, startBucket);
    var endTuple = Tuple.from(universalPath, endBucket + 1);
    return new Tuple[] {startTuple, endTuple};
  }
}
