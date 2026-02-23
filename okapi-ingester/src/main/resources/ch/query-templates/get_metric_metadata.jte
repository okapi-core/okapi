@param org.okapi.promql.ch.ChMetricMetadataQueryTemplate data

SELECT metric, event_type
FROM ${data.getTable()}
PREWHERE ts_end >= toDateTime64(${data.getStartMs()}/1000.0, 3, 'UTC')
AND ts_end <= toDateTime64(${data.getEndMs()}/1000.0, 3, 'UTC')
@if(data.getMetric() != null)
AND metric = '${data.getMetric()}'
@endif
GROUP BY metric, event_type
ORDER BY metric
LIMIT ${data.getLimit()}
