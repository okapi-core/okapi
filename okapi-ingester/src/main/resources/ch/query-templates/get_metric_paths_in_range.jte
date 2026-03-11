@param org.okapi.metrics.ch.template.ChSearchMetricsQueryTemplate data

SELECT DISTINCT metric, tags
FROM ${data.getTable()}
PREWHERE ts_start <= toDateTime64(${data.getEndMs()}/1000.0, 3, 'UTC')
AND ts_end >= toDateTime64(${data.getStartMs()}/1000.0, 3, 'UTC')
LIMIT 10000
