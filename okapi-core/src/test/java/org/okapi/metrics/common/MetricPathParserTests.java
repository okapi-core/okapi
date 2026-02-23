package org.okapi.metrics.common;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.okapi.metrics.common.MetricsPathParser.ParsedHashKey;

public class MetricPathParserTests {

  public static final Stream<Arguments> parseableMetrics() {
    return Stream.of(
        Arguments.of(
            "tenantId:http_requests{request=400}",
            new MetricsPathParser.MetricsRecord(
                "tenantId", "http_requests", Map.of("request", "400"))),
        Arguments.of(
            "tenantId:http_requests{request=blah, request2=blah}",
            new MetricsPathParser.MetricsRecord(
                "tenantId", "http_requests", Map.of("request", "blah", "request2", "blah"))),
        Arguments.of(
            "tenantId:http_requests{}",
            new MetricsPathParser.MetricsRecord(
                "tenantId", "http_requests", Collections.emptyMap())));
  }

  public static final Stream<Arguments> unParseableMetrics() {
    return Stream.of(
        Arguments.of("http_requests{request=400}"),
        Arguments.of("http_requests{request=blah, request2=blah}"),
        Arguments.of("http_requests{}"));
  }

  static Stream<Arguments> hashKeyTestCases() {
    return Stream.of(
        // ✅ Valid cases
        Arguments.of(
            "tenantId:series-A{label_A=value_A}:s:1754453239",
            new ParsedHashKey(
                "tenantId", "series-A", Map.of("label_A", "value_A"), "s", 1754453239L)),
        Arguments.of(
            "tenantId:series-A{label_A=value_A}:h:6553",
            new ParsedHashKey("tenantId", "series-A", Map.of("label_A", "value_A"), "h", 6553L)),
        Arguments.of(
            "tenantId:series-A{label_A=value_A}:m:60",
            new MetricsPathParser.ParsedHashKey(
                "tenantId", "series-A", Map.of("label_A", "value_A"), "m", 60L)),

        // ❌ Invalid cases
        Arguments.of("tenantId_series-A{label_A=value_A}:s:123", null),
        Arguments.of("tenantId:series-A{label_A=value_A:s:123", null),
        Arguments.of("tenantId:series-A{label_A=value_A}::123", null),
        Arguments.of("tenantId:series-A{label_A=value_A}:x:123", null),
        Arguments.of("tenantId:series-A{label_A=value_A}:s:", null),
        Arguments.of("tenantId:series-A{label_A=value_A}:s:abc", null),
        Arguments.of("tenantId:series-A{label_A=value_A}s:123", null),
        Arguments.of("", null));
  }

  @ParameterizedTest
  @MethodSource("parseableMetrics")
  public void testMetricPaths(String path, MetricsPathParser.MetricsRecord expected) {
    var parsed = MetricsPathParser.parse(path);
    assertEquals(expected.tenantId(), parsed.get().tenantId());
    assertEquals(expected.name(), parsed.get().name());
    assertEquals(expected.tags(), parsed.get().tags());
  }

  @ParameterizedTest
  @MethodSource("unParseableMetrics")
  public void testMetricPaths(String path) {
    var parsed = MetricsPathParser.parse(path);
    assertTrue(parsed.isEmpty());
  }

  @ParameterizedTest
  @MethodSource("hashKeyTestCases")
  void testParseHashKey(String input, MetricsPathParser.ParsedHashKey expected) {
    Optional<MetricsPathParser.ParsedHashKey> actual = MetricsPathParser.parseHashKey(input);

    if (expected == null) {
      assertTrue(actual.isEmpty(), "Expected parse to fail for input: " + input);
    } else {
      assertTrue(actual.isPresent(), "Expected parse to succeed for input: " + input);
      ParsedHashKey actualValue = actual.get();
      assertEquals(expected.tenantId(), actualValue.tenantId());
      assertEquals(expected.name(), actualValue.name());
      assertEquals(expected.tags(), actualValue.tags());
      assertEquals(expected.resolution(), actualValue.resolution());
      assertEquals(expected.value(), actualValue.value());
    }
  }
}
