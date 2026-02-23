@param org.okapi.promql.ch.ChGetGaugeRawQueryTemplate data

SELECT toUnixTimestamp64Milli(timestamp) AS ts_ms, value
FROM ${data.getTable()}
@if(data.getResource() != null)
PREWHERE resource = '${data.getResource()}'
@else
PREWHERE 1
@endif
AND metric = '${data.getMetric()}'
AND timestamp >= toDateTime64(${data.getStartMs()}/1000.0, 3, 'UTC')
AND timestamp <= toDateTime64(${data.getEndMs()}/1000.0, 3, 'UTC')
@for(var tag : data.getTags().entrySet())
    AND mapContains(tags, '${tag.getKey()}') AND tags['${tag.getKey()}'] = '${tag.getValue()}'
@endfor
ORDER BY ts_ms
