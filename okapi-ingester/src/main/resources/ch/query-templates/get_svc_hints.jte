@param org.okapi.metrics.ch.MetricHintsQueryTemplate data

SELECT DISTINCT svc
FROM ${data.getTable()}
WHERE 1 = 1
@if(data.getEventType() != null && !data.getEventType().isBlank())
    AND event_type = '${data.getEventType()}'
@endif
@if(data.getSvcPrefix() != null && !data.getSvcPrefix().isBlank())
    AND svc LIKE '${data.getSvcPrefix()}%'
@endif
AND ts_start < toDateTime64(${data.getEndMs()}/1000.0, 3, 'UTC')
AND ts_end > toDateTime64(${data.getStartMs()}/1000.0, 3, 'UTC')
ORDER BY svc
LIMIT ${data.getLimit()}
