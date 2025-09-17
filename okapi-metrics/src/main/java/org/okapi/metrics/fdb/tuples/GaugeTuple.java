package org.okapi.metrics.fdb.tuples;

import com.apple.foundationdb.tuple.Tuple;
import java.util.Objects;
import java.util.Random;
import lombok.ToString;
import lombok.Value;
import org.okapi.metrics.fdb.SubspaceFactory;

@Value
@ToString
public class GaugeTuple implements FdbTuple {

  String universalPath;
  Long bucket;
  String nodeId;

  int uuid;
  BUCKET_TYPE type;
  Random random;

  public GaugeTuple(String universalPath, BUCKET_TYPE type, Long bucket, String nodeId) {
    this.universalPath = universalPath;
    this.bucket = bucket;
    this.nodeId = nodeId;
    this.type = type;
    this.random = new Random();
    this.uuid = this.random.nextInt(Integer.MAX_VALUE);
  }

  public Tuple toTuple() {
    return Tuple.from(universalPath, this.type.name(), bucket, nodeId, uuid);
  }

  @Override
  public byte[] pack() {
    var subspace = SubspaceFactory.getGaugeSub();
    return subspace.pack(toTuple());
  }


  public static GaugeTuple fromKey(byte[] key) {
    var subspace = SubspaceFactory.getGaugeSub();
    var tuple = subspace.unpack(key);
    // the first two are okapi, and metrics
    var path = tuple.getString(0);
    var bucketType = BUCKET_TYPE.valueOf(tuple.getString(1));
    var bucket = tuple.getLong(2);
    var nodeId = tuple.getString(3);
    return new GaugeTuple(path, bucketType, bucket, nodeId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    GaugeTuple that = (GaugeTuple) o;
    return uuid == that.uuid
        && Objects.equals(universalPath, that.universalPath)
        && Objects.equals(bucket, that.bucket)
        && Objects.equals(nodeId, that.nodeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(universalPath, bucket, nodeId, uuid);
  }
}
