@param org.okapi.metrics.ch.template.ChGetHistoQueryTemplate data

SELECT resource, metric_name, tags,
toUnixTimestamp64Milli(ts_start) AS ts_start_ms,
toUnixTimestamp64Milli(ts_end) AS ts_end_ms,
buckets, counts, toString(histo_type) AS histo_type
FROM ${data.getTable()}
PREWHERE resource = '${data.getResource()}'
AND metric_name = '${data.getMetric()}'
AND ts_start < toDateTime64(${data.getTe()}/1000.0, 3, 'UTC')
AND ts_end > toDateTime64(${data.getTs()}/1000.0, 3, 'UTC')
AND histo_type = '${data.getHistoType()}'
@if(data.getTags() != null && !data.getTags().isEmpty())
WHERE 1
@for(var tag : data.getTags().entrySet())
AND mapContains(tags, '${tag.getKey()}') AND tags['${tag.getKey()}'] = '${tag.getValue()}'
@endfor
@endif
