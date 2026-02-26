@param org.okapi.traces.ch.template.ChServiceRedOpMetricsTemplate data

SELECT
    ${data.getBucketStartExpr()} AS bucket_start_ms,
    count() AS total_count,
    countIf(span_status = 'ERROR') AS error_count,
    quantileIf(0.50)(${data.getDurationExpr()}, span_status = 'OK' AND ${data.getDurationExpr()} > 0) AS duration_p50,
    quantileIf(0.75)(${data.getDurationExpr()}, span_status = 'OK' AND ${data.getDurationExpr()} > 0) AS duration_p75,
    quantileIf(0.90)(${data.getDurationExpr()}, span_status = 'OK' AND ${data.getDurationExpr()} > 0) AS duration_p90,
    quantileIf(0.99)(${data.getDurationExpr()}, span_status = 'OK' AND ${data.getDurationExpr()} > 0) AS duration_p99
FROM ${data.getTable()}
WHERE 1=1
@if(data.getServiceName() != null && !data.getServiceName().isEmpty())
  AND service_name = '${data.getServiceName()}'
@endif
@if(data.getSpanName() != null && !data.getSpanName().isEmpty())
  AND span_name = '${data.getSpanName()}'
@endif
@if(data.getTimestampFilter() != null)
  AND ts_start_nanos >= ${data.getTimestampFilter().getTsStartNanos()}
  AND ts_end_nanos <= ${data.getTimestampFilter().getTsEndNanos()}
@endif
GROUP BY bucket_start_ms
ORDER BY bucket_start_ms
