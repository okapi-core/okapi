package org.okapi.metrics.search;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.okapi.metrics.common.MetricsPathParser.MetricsRecord;

public class MetricsSearcherTest {

  private static final List<String> SAMPLE_METRICS =
      List.of(
          "tenantA:http_200{service=app,region=us-west}",
          "tenantA:http_400{service=app,region=us-west}",
          "tenantA:http_401{service=db,region=us-west}",
          "tenantA:http_500{service=app,region=us-east}",
          "tenantB:http_400{service=app,region=us-west}", // Different tenant
          "tenantA:cpu_usage{core=1,region=us-west}",
          "tenantA:disk_io{device=sda1,region=us-east}",
          "tenantC:http_401{service=db,region=us-west}");

  private static Stream<Arguments> testCases() {
    return Stream.of(
        Arguments.of(
            "http_*{service=app}",
            "tenantA",
            Set.of(
                "tenantA:http_200{region=us-west,service=app}",
                "tenantA:http_400{region=us-west,service=app}",
                "tenantA:http_500{region=us-east,service=app}"),
            "Matches http_* with exact label"),
        Arguments.of(
            "http_4*{service=*}",
            "tenantA",
            Set.of(
                "tenantA:http_400{region=us-west,service=app}",
                "tenantA:http_401{region=us-west,service=db}"),
            "Wildcard match on metric name and label value wildcard"),
        Arguments.of(
            "http_*{service=db}",
            "tenantA",
            Set.of("tenantA:http_401{region=us-west,service=db}"),
            "Exact match on label value"),
        Arguments.of(
            "http_*{service=ap*}",
            "tenantA",
            Set.of(
                "tenantA:http_200{region=us-west,service=app}",
                "tenantA:http_400{region=us-west,service=app}",
                "tenantA:http_500{region=us-east,service=app}"),
            "Prefix match on label value"),
        Arguments.of(
            "http_*{service*}",
            "tenantA",
            Set.of(
                "tenantA:http_200{region=us-west,service=app}",
                "tenantA:http_400{region=us-west,service=app}",
                "tenantA:http_401{region=us-west,service=db}",
                "tenantA:http_500{region=us-east,service=app}"),
            "Prefix match on label key only"),
        Arguments.of(
            "*{region=us-west}",
            "tenantA",
            Set.of(
                "tenantA:http_200{region=us-west,service=app}",
                "tenantA:http_400{region=us-west,service=app}",
                "tenantA:http_401{region=us-west,service=db}",
                "tenantA:cpu_usage{core=1,region=us-west}"),
            "Wildcard metric name with exact region"),
        Arguments.of(
            "cpu_*{core=*}",
            "tenantA",
            Set.of("tenantA:cpu_usage{core=1,region=us-west}"),
            "Match core=* on cpu_*"),
        Arguments.of(
            "*",
            "tenantB",
            Set.of("tenantB:http_400{region=us-west,service=app}"),
            "Wildcard match, different tenant"),
        Arguments.of(
            "http_*{service=db}",
            "tenantC",
            Set.of("tenantC:http_401{region=us-west,service=db}"),
            "Single match for tenantC"),
        Arguments.of("http_*{service=db}", "tenantZ", Set.of(), "No match for missing tenant"));
  }

  private static String formatTags(Map<String, String> tags) {
    return tags.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(() -> new StringJoiner(",", "{", "}"), StringJoiner::add, StringJoiner::merge)
        .toString();
  }

  @ParameterizedTest(name = "{index}: {3}")
  @MethodSource("testCases")
  void testSearchMatchingMetrics(
      String pattern, String tenantId, Set<String> expected, String description) {
    List<MetricsRecord> result =
        MetricsSearcher.searchMatchingMetrics(tenantId, SAMPLE_METRICS, pattern);
    Set<String> actual = new HashSet<>();
    for (MetricsRecord r : result) {
      actual.add(r.tenantId() + ":" + r.name() + formatTags(r.tags()));
    }
    assertEquals(expected, actual);
  }
}
