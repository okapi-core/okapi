package org.okapi.traces.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.okapi.traces.model.OkapiSpan;

public interface TraceRepository {

  void saveBatch(List<OkapiSpan> okapiSpans);

  List<OkapiSpan> getSpansByTraceId(String traceId, String tenant);

  Optional<OkapiSpan> getSpanById(String spanId, String tenant);

  List<OkapiSpan> listSpansByDuration(String tenant, long startMillis, long endMillis, int limit);

  // Traces present in the window (unique traceIds) and count of error traces
  Map<String, Object> listTracesByWindow(String tenant, long startMillis, long endMillis);

  List<OkapiSpan> listErrorSpans(String tenant, long startMillis, long endMillis, int limit);

  // Histogram keyed by minute_bucket -> {"ok": count, "error": count}
  Map<Long, Map<String, Long>> spanHistogramByMinute(
      String tenant, long startMillis, long endMillis);
}
