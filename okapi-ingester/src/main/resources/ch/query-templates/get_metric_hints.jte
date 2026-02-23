@param org.okapi.metrics.ch.MetricHintsQueryTemplate data

SELECT DISTINCT metric
FROM ${data.getTable()}
WHERE 1 = 1
@if(data.getSvc() != null && !data.getSvc().isBlank())
AND svc = '${data.getSvc()}'
@endif
@if(data.getEventType() != null && !data.getEventType().isBlank())
AND event_type = '${data.getEventType()}'
@endif
@if(data.getMetricPrefix() != null && !data.getMetricPrefix().isBlank())
AND metric LIKE '${data.getMetricPrefix()}%'
@endif
AND ts_start < toDateTime64(${data.getEndMs()}/1000.0, 3, 'UTC')
AND ts_end > toDateTime64(${data.getStartMs()}/1000.0, 3, 'UTC')
ORDER BY metric
LIMIT ${data.getLimit()}
