@param org.okapi.resources.ResourceSearchQueryTemplate data

SELECT DISTINCT resource
FROM ${data.getTable()}
WHERE ts_start < toDateTime64(${data.getEndMs()}/1000.0, 3, 'UTC')
AND ts_end > toDateTime64(${data.getStartMs()}/1000.0, 3, 'UTC')
