package org.okapi.promql;

import com.google.re2j.Pattern;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.ts.SeriesDiscovery;
import org.okapi.promql.parse.LabelMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MockSeriesDiscovery implements SeriesDiscovery {
  private final List<SeriesId> known;

  public MockSeriesDiscovery(List<SeriesId> known) {
    this.known = List.copyOf(known);
  }

  @Override
  public List<SeriesId> expand(String metricOrNull, List<LabelMatcher> matchers) {
    List<SeriesId> out = new ArrayList<>();
    for (SeriesId s : known) {
      if (metricOrNull != null && !metricOrNull.equals(s.metric())) continue;
      if (matchesAll(s.metric(), s.labels().tags(), matchers)) out.add(s);
    }
    return out;
  }

  private static boolean matchesAll(String metricOrNull, Map<String, String> labels, List<LabelMatcher> matchers) {
    if (matchers == null || matchers.isEmpty()) return true;
    for (LabelMatcher m : matchers) {
      String actual = m.name().equals("__name__") ? metricOrNull : labels.get(m.name());
      switch (m.op()) {
        case EQ -> {
          if (actual == null || !actual.equals(m.value())) return false;
        }
        case NE -> {
          if (actual != null && actual.equals(m.value())) return false;
        }
        case RE -> {
          if (actual == null) return false;
          if (!Pattern.compile(m.value()).matches(actual)) return false;
        }
        case NRE -> {
          if (actual != null && Pattern.compile(m.value()).matches(actual)) return false;
        }
      }
    }
    return true;
  }
}
