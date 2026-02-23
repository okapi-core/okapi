@param org.okapi.promql.ch.ChMetricEventTypeQueryTemplate data

SELECT event_type
FROM ${data.getTable()}
@if(data.getResource() != null)
PREWHERE svc = '${data.getResource()}'
@else
PREWHERE 1
@endif
AND metric = '${data.getMetric()}'
AND ts_start < toDateTime64(${data.getEndMs()}/1000.0, 3, 'UTC')
AND ts_end > toDateTime64(${data.getStartMs()}/1000.0, 3, 'UTC')
@for(var tag : data.getTags().entrySet())
    AND mapContains(tags, '${tag.getKey()}') AND tags['${tag.getKey()}'] = '${tag.getValue()}'
@endfor
LIMIT 1
