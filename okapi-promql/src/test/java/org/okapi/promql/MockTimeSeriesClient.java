package org.okapi.promql;

import org.okapi.Statistics;
import org.okapi.promql.eval.ts.RESOLUTION;
import org.okapi.promql.eval.ts.TimeseriesClient;

import java.util.*;
import java.util.stream.Collectors;

public final class MockTimeSeriesClient implements TimeseriesClient {

    public static final class Key {
        public final String metric;
        public final Map<String,String> tags;
        public Key(String metric, Map<String,String> tags) {
            this.metric = metric;
            this.tags = Collections.unmodifiableMap(new HashMap<>(tags));
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(metric, k.metric) && Objects.equals(tags, k.tags);
        }
        @Override public int hashCode() { return Objects.hash(metric, tags); }
    }

    // Backing store: (metric,tags) -> timestamp -> Statistics
    private final Map<Key, NavigableMap<Long, Statistics>> store = new HashMap<>();

    public void put(String metric, Map<String,String> tags, long ts, Statistics stats) {
        var key = new Key(metric, tags);
        var series = store.computeIfAbsent(key, k -> new TreeMap<>());
        series.put(ts, stats);
    }

    @Override
    public Map<Long, Statistics> get(String name, Map<String, String> tags,
                                     RESOLUTION res, long startMs, long endMs) {
        var key = new Key(name, tags == null ? Map.of() : tags);
        var series = store.get(key);
        if (series == null) return Map.of();
        // inclusive range
        return series.subMap(startMs, true, endMs, true)
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}