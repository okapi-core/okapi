package org.okapi.promql.eval;

import org.okapi.Statistics;

import java.util.List;
import java.util.Map;

public class VectorData {
    public record Labels(Map<String, String> tags) {}

    public record SeriesId(String metric, Labels labels) {}

    public record Sample(long ts, float value) {}

    public record SeriesSample(SeriesId id, Sample sample) {
        public SeriesId series(){
            return id;
        }
    }

    public record StatsPoint(long ts, Statistics stats) {} // backed by your storage

    public record SeriesWindow(SeriesId id, List<StatsPoint> points) {}
}
