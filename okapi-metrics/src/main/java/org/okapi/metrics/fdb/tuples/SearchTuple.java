package org.okapi.metrics.fdb.tuples;

import com.apple.foundationdb.tuple.Tuple;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.okapi.metrics.fdb.SubspaceFactory;

import java.util.Objects;

@AllArgsConstructor
@ToString
public class SearchTuple implements FdbTuple {
  @Getter String tenant;
  @Getter String localPath;
  @Getter Long bucket;

  @Override
  public byte[] pack() {
    var searchSub = SubspaceFactory.getMetricsSearchSub();
    return searchSub.pack(Tuple.from(tenant, bucket, localPath));
  }

  public static byte[][] searchRange(String tenant, long blockStart, long blockEnd) {
    var searchSub = SubspaceFactory.getMetricsSearchSub();
    return new byte[][] {
      searchSub.pack(Tuple.from(tenant, blockStart)),
      searchSub.pack(Tuple.from(tenant, 1 + blockEnd)),
    };
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    SearchTuple that = (SearchTuple) o;
    return Objects.equals(tenant, that.tenant)
        && Objects.equals(localPath, that.localPath)
        && Objects.equals(bucket, that.bucket);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenant, localPath, bucket);
  }

  public static SearchTuple fromKey(byte[] key) {
    var searchSub = SubspaceFactory.getMetricsSearchSub();
    var tuple = searchSub.unpack(key);
    var tenant = tuple.getString(0);
    var bucket = tuple.getLong(1);
    var path = tuple.getString(2);
    return new SearchTuple(tenant, path, bucket);
  }

}
