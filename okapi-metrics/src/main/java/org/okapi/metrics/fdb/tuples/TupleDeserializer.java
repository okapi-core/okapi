package org.okapi.metrics.fdb.tuples;

import com.apple.foundationdb.tuple.Tuple;
import java.util.Optional;
import org.okapi.metrics.fdb.SubspaceFactory;

public class TupleDeserializer {

  public static Optional<GaugeTuple> readGaugeTuple(byte[] bytes) {
    try {
      if (bytes == null || bytes.length == 0) return Optional.empty();
      var t = SubspaceFactory.getGaugeSub().unpack(bytes);
      var path = t.getString(0);
      var bucketType = BUCKET_TYPE.valueOf(t.getString(1));
      var bucket = t.getLong(2);
      var nodeId = t.getString(3);
      return Optional.of(new GaugeTuple(path, bucketType, bucket, nodeId));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static Optional<HistoTuple> readHistoTuple(byte[] bytes) {
    try {
      if (bytes == null || bytes.length == 0) return Optional.empty();
      var t = SubspaceFactory.getHistoSubspace().unpack(bytes);
      var path = t.getString(0);
      var start = t.getLong(1);
      var end = t.getLong(2);
      Object ubOrInf = t.get(3);
      boolean inf;
      Float ub = null;
      String nodeId;
      if (ubOrInf instanceof String s && "inf".equals(s)) {
        inf = true;
        nodeId = t.getString(4);
      } else {
        inf = false;
        if (ubOrInf instanceof Double d) ub = d.floatValue();
        else if (ubOrInf instanceof Float f) ub = f;
        else if (ubOrInf instanceof Long l) ub = l.floatValue();
        else if (ubOrInf instanceof Integer i) ub = i.floatValue();
        else if (ubOrInf != null) ub = Float.valueOf(ubOrInf.toString());
        nodeId = t.getString(4);
      }
      return Optional.of(new HistoTuple(path, ub, inf, start, end, nodeId));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static Optional<DeltaSumTuple> readDeltaSumTuple(byte[] bytes) {
    try {
      if (bytes == null || bytes.length == 0) return Optional.empty();
      var t = SubspaceFactory.getSumSubspace().unpack(bytes);
      var path = t.getString(0);
      var start = t.getLong(1);
      var end = t.getLong(2);
      var nodeId = t.getString(3);
      return Optional.of(new DeltaSumTuple(path, nodeId, start, end));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static Optional<CsumTuple> readCsumTuple(byte[] bytes) {
    try {
      if (bytes == null || bytes.length == 0) return Optional.empty();
      var t = SubspaceFactory.getCsumSubspace().unpack(bytes);
      var path = t.getString(0);
      var start = t.getLong(1);
      var end = t.getLong(2);
      return Optional.of(new CsumTuple(path, start, end));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
