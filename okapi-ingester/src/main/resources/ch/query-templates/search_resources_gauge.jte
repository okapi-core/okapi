@param org.okapi.resources.ResourceSearchQueryTemplate data

SELECT DISTINCT resource
FROM ${data.getTable()}
WHERE timestamp >= toDateTime64(${data.getStartMs()}/1000.0, 3, 'UTC')
AND timestamp < toDateTime64(${data.getEndMs()}/1000.0, 3, 'UTC')
