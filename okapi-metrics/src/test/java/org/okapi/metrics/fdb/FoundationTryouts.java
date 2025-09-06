package org.okapi.metrics.fdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.apple.foundationdb.FDB;
import com.apple.foundationdb.subspace.Subspace;
import com.apple.foundationdb.tuple.Tuple;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.okapi.Statistics;
import org.okapi.fixtures.ReadingGenerator;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.stats.*;

public class FoundationTryouts {

  @Test
  public void testLoadStore() {
    System.load("/usr/local/lib/libfdb_c.dylib");
    var fdb = FDB.selectAPIVersion(720);
    var db = fdb.open();
    var sub = new Subspace(Tuple.from("time"));
    var key = sub.pack(Tuple.from("min"));
    var value = Longs.toByteArray(1000L);
    db.run(
        tr -> {
          tr.set(key, value);
          return null;
        });
    var result = db.read(tr -> tr.get(key).join());
    var val = Longs.fromByteArray(result);
    assertEquals(1000L, val);
  }

  @Test
  public void testLoadStoreStatistics() throws StatisticsFrozenException, InterruptedException {
    System.load("/usr/local/lib/libfdb_c.dylib");
    var fdb = FDB.selectAPIVersion(720);
    var db = fdb.open();
    var sub = new Subspace(Tuple.from("metrics"));
    var generator = new ReadingGenerator(Duration.of(100, ChronoUnit.MILLIS), 60 * 5);
    var ds = generator.populateRandom(0.f, 1f);
    var path = "metric_path_{}" + System.currentTimeMillis();
    var stats = new HashMap<String, UpdatableStatistics>();
    var supplier = new KllStatSupplier();
    var ctx = MetricsContext.createContext("test");
    for (int i = 0; i < ds.getValues().size(); i++) {
      var secondHash = ds.getTimestamps().get(i) / 1000L;
      var hashPath = path + "/" + secondHash;
      var stat = stats.computeIfAbsent(hashPath, (k) -> supplier.get());
      stat.update(ctx, ds.getValues().get(i));
    }

    var now = System.currentTimeMillis();
    db.run(
        tr -> {
          for (var k : stats.keySet()) {
            var split = k.split("/");
            var mp = split[0];
            var bucket = Long.parseLong(split[1]);
            var stat = stats.get(k);
            var tuple = sub.pack(Tuple.from(mp, bucket));
            tr.set(tuple, stat.serialize());
          }
          return null;
        });

    var st = ds.getTimestamps().stream().reduce(Long.MIN_VALUE, Math::min);
    var end = ds.getTimestamps().stream().reduce(Long.MAX_VALUE, Math::max);
    var restorer = new ReadonlyRestorer();
    var reading =
        db
            .run(
                tr -> {
                  var rangeStart = sub.pack(Tuple.from(path, st));
                  var rangeEnd = sub.pack(Tuple.from(path, end));
                  return tr.getRange(rangeStart, rangeEnd).asList().join();
                })
            .stream()
            .map(
                r -> {
                  var key = Tuple.from(r.getKey());
                  return restorer.deserialize(r.getValue());
                })
            .map(Statistics::avg)
            .toList();
    var reduction = ds.avgReduction(RES_TYPE.SECONDLY);
    Assertions.assertEquals(reduction.getValues(), reading);
  }

  @Test
  public void canGroupByTuple() {
    var map = new HashMap<Tuple, String>();
    var tuple1 = Tuple.from("x", "y");
    map.put(tuple1, "z");
    var tuple2 = Tuple.from("x", "y2");
    map.put(tuple2, "z2");
    var q = Tuple.from("x", "y");
    var v = map.get(q);
    assertEquals("z", v);
    var q2 = Tuple.from("x", "y2");
    var v2 = map.get(q2);
    assertEquals("z2", v2);
  }

  @Test
  public void canGroupByTuple2() {
    var map = HashMultimap.<Tuple, String>create();
    var tuple1 = Tuple.from("x", "y");
    map.put(tuple1, "z");
    var tuple2 = Tuple.from("x", "y");
    map.put(tuple2, "z2");
    var q = Tuple.from("x", "y");
    var v = map.get(q);
    assertEquals(Sets.newHashSet("z", "z2"), v);
  }
}
