@param org.okapi.traces.ch.ChSpansAttributeHintsQueryTemplate data

SELECT DISTINCT
    attribute_name,
    attribute_type
FROM ${data.getTable()}
WHERE 1=1
@if(data.getTsStartNs() != null)
    AND ts_start_ns >= ${data.getTsStartNs()}
@endif
@if(data.getTsEndNs() != null)
    AND ts_end_ns <= ${data.getTsEndNs()}
@endif
LIMIT ${data.getLimit()}
