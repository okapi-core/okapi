package org.okapi.metrics.fdb.tuples;

import com.apple.foundationdb.tuple.Tuple;
import lombok.AllArgsConstructor;
import org.okapi.metrics.fdb.SubspaceFactory;

import java.util.Objects;

@AllArgsConstructor
public class SearchTuple {
  String universalPath;
  Long bucket;

  public byte[] toKey() {
    var searchSub = SubspaceFactory.getMetricsSearchSub();
    return searchSub.pack(Tuple.from(universalPath, bucket));
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    SearchTuple that = (SearchTuple) o;
    return Objects.equals(universalPath, that.universalPath) && Objects.equals(bucket, that.bucket);
  }

  @Override
  public int hashCode() {
    return Objects.hash(universalPath, bucket);
  }
}
