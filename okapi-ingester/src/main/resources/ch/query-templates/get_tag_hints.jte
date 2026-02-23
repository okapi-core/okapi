@param org.okapi.metrics.ch.MetricHintsQueryTemplate data

SELECT DISTINCT tag_key
FROM ${data.getTable()}
ARRAY JOIN mapKeys(tags) AS tag_key
WHERE 1 = 1
@if(data.getSvc() != null && !data.getSvc().isBlank())
AND svc = '${data.getSvc()}'
@endif
@if(data.getMetric() != null && !data.getMetric().isBlank())
AND metric = '${data.getMetric()}'
@endif
@if(data.getEventType() != null && !data.getEventType().isBlank())
AND event_type = '${data.getEventType()}'
@endif
@if(data.getTagPrefix() != null && !data.getTagPrefix().isBlank())
AND tag_key LIKE '${data.getTagPrefix()}%'
@endif
AND ts_start < toDateTime64(${data.getEndMs()}/1000.0, 3, 'UTC')
AND ts_end > toDateTime64(${data.getStartMs()}/1000.0, 3, 'UTC')
@if(data.getOtherTags() != null && !data.getOtherTags().isEmpty())
@for(var tag : data.getOtherTags().entrySet())
AND mapContains(tags, '${tag.getKey()}') AND tags['${tag.getKey()}'] = '${tag.getValue()}'
@endfor
@endif
ORDER BY tag_key
LIMIT ${data.getLimit()}
