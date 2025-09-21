package org.okapi.rest.metrics;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.okapi.rest.metrics.payloads.Gauge;

public class ExportMetricsRequestTest {

  private static Map<String, String> unsortedTags() {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("b", "2");
    m.put("a", "1");
    m.put("c", "3");
    return m;
  }

  private static List<String> keys(Map<String, String> m) {
    return new ArrayList<>(m.keySet());
  }

  @Test
  public void builderSortsTags() {
    ExportMetricsRequest r =
        ExportMetricsRequest.builder()
            .tenantId("t")
            .metricName("m")
            .type(MetricType.GAUGE)
            .tags(unsortedTags())
            .gauge(Gauge.builder().ts(new long[] {1}).value(new float[] {1.0f}).build())
            .build();

    assertNotNull(r.getTags());
    assertTrue(r.getTags() instanceof TreeMap, "tags should be a TreeMap");
    assertEquals(List.of("a", "b", "c"), keys(r.getTags()));
  }

  @Test
  public void setterSortsTags() {
    ExportMetricsRequest r = new ExportMetricsRequest();
    r.setTenantId("t");
    r.setMetricName("m");
    r.setType(MetricType.GAUGE);
    r.setTags(unsortedTags());

    assertNotNull(r.getTags());
    assertTrue(r.getTags() instanceof TreeMap);
    assertEquals(List.of("a", "b", "c"), keys(r.getTags()));
  }

  @Test
  public void allArgsConstructorSortsTags() {
    ExportMetricsRequest r =
        new ExportMetricsRequest(
            "t", "m", unsortedTags(), MetricType.GAUGE, null, null, null);
    assertNotNull(r.getTags());
    assertTrue(r.getTags() instanceof TreeMap);
    assertEquals(List.of("a", "b", "c"), keys(r.getTags()));
  }

  @Test
  public void jsonDeserializationUsesSortedTags() throws Exception {
    String json =
        "{"
            + "\"tenantId\":\"t\"," 
            + "\"metricName\":\"m\"," 
            + "\"type\":\"GAUGE\"," 
            + "\"tags\":{\"b\":\"2\",\"a\":\"1\",\"c\":\"3\"}"
            + "}";
    ObjectMapper mapper = new ObjectMapper();
    ExportMetricsRequest r = mapper.readValue(json, ExportMetricsRequest.class);

    assertNotNull(r.getTags());
    assertTrue(r.getTags() instanceof TreeMap);
    assertEquals(List.of("a", "b", "c"), keys(r.getTags()));
  }
}

