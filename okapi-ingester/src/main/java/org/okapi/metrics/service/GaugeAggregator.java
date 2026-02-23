package org.okapi.metrics.service;

import com.google.common.collect.TreeMultimap;
import java.util.Collection;
import java.util.List;
import org.apache.datasketches.kll.KllFloatsSketch;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.query.SketchToResConverter;
import org.okapi.primitives.TimestampedReadonlySketch;
import org.okapi.rest.metrics.query.GetGaugeResponse;

public class GaugeAggregator {
  public static final GetGaugeResponse aggregateSketches(
      List<TimestampedReadonlySketch> sketches, RES_TYPE resType, AGG_TYPE aggType) {
    var times = sketches.stream().map(TimestampedReadonlySketch::getTs).toList();
    TreeMultimap<Long, TimestampedReadonlySketch> map = TreeMultimap.create();
    for (var sk : sketches) {
      map.put(sk.getTs(), sk);
    }
    var ts = map.keySet().stream().toList();
    var values =
        ts.stream()
            .map(
                s -> {
                  var merged = merge(s, map.get(s));
                  var aggregated =
                      SketchToResConverter.getGaugeResult(merged.getReadOnlySketch(), aggType);
                  return aggregated;
                })
            .toList();
    return GetGaugeResponse.builder()
        .resolution(resType)
        .aggregation(aggType)
        .times(ts)
        .values(values)
        .build();
  }

  public static TimestampedReadonlySketch merge(
      long ts, Collection<TimestampedReadonlySketch> sketches) {
    var sum = 0.0f;
    var count = 0L;
    var dev = 0.0f;
    var kllSketch = KllFloatsSketch.newHeapInstance();
    for (var sk : sketches) {
      var readOnlySketch = sk.getReadOnlySketch();
      sum += readOnlySketch.getMean() * readOnlySketch.getCount();
      count += readOnlySketch.getCount();
      dev += readOnlySketch.getSumOfDeviationsSquared();
      readOnlySketch.mergeInto(kllSketch);
    }
    var mean = sum / count;
    return new TimestampedReadonlySketch(
        ts, new org.okapi.primitives.ReadOnlySketch(mean, count, dev, kllSketch.toByteArray()));
  }
}
