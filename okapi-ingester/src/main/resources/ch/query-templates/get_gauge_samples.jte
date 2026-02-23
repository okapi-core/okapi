@param org.okapi.metrics.ch.ChGetGaugeQueryTemplate data

SELECT ${data.getBucketExpr()} AS bucket_ms, ${data.getAggExpr()} AS value
FROM ${data.getTable()}
WHERE metric = '${data.getMetric()}' AND resource = '${data.getResource()}'
AND timestamp >= toDateTime64(${data.getStartMs()}/1000.0, 3, 'UTC')
AND timestamp < toDateTime64(${data.getEndMs()}/1000.0, 3, 'UTC')
@for(var tag : data.getTags().entrySet())
    AND mapContains(tags, '${tag.getKey()}') AND tags['${tag.getKey()}'] = '${tag.getValue()}'
@endfor
GROUP BY bucket_ms ORDER BY bucket_ms
