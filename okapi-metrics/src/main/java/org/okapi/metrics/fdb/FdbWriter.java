package org.okapi.metrics.fdb;

import com.apple.foundationdb.Database;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.SharedMessageBox;

@Slf4j
@AllArgsConstructor
public class FdbWriter {
  public static final String FDB_CONSUMER = FdbWriter.class.getSimpleName();
  public static final int MAX_TX_SIZE = 1024 * 1024 * 10;

  Database db;
  SharedMessageBox<FdbTx> consumerBox;

  public List<int[]> batchTx(List<FdbTx> txQueue) {
    var batchPoints = new ArrayList<int[]>();
    int st = 0;
    int acc = 0;
    for (int i = 0; i < txQueue.size(); i++) {
      int sz = txQueue.get(i).size();
      if (acc + sz > MAX_TX_SIZE) {
        // Close current batch [st, i)
        if (st < i) {
          batchPoints.add(new int[] {st, i});
        }
        // Start a new batch at i
        st = i;
        acc = 0;
      }
      acc += sz;
    }
    // Add final batch if any elements remain
    if (st < txQueue.size()) {
      batchPoints.add(new int[] {st, txQueue.size()});
    }
    return batchPoints;
  }

  public void doTxBatch(int[] cutPoints, List<FdbTx> Q) {
    db.run(
        tr -> {
          for (int i = cutPoints[0]; i < cutPoints[1]; i++) {
            var tx = Q.get(i);
            tr.set(tx.getKey(), tx.getVal());
          }

          return null;
        });
  }

  public void writeOnce() {
    log.debug("Consuming from box");
    while (!consumerBox.isEmpty()) {
      var sink = new ArrayList<FdbTx>();
      consumerBox.drain(sink, FDB_CONSUMER);
      // group by key -> secondly minutely hourly buckets
      var batch = batchTx(sink);
      for (var cutPoints : batch) {
        doTxBatch(cutPoints, sink);
      }
    }
  }
}
