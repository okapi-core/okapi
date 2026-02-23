@param org.okapi.traces.ch.ChSpanAttributeValuesCustomTemplate data

SELECT DISTINCT
  ${data.getValueExpr()} AS value
FROM ${data.getTable()}
WHERE 1=1
@if(data.getTsStartNs() != null)
  AND ts_start_ns >= ${data.getTsStartNs()}
@endif
@if(data.getTsEndNs() != null)
  AND ts_end_ns <= ${data.getTsEndNs()}
@endif
AND isNotNull(${data.getValueExpr()})
LIMIT ${data.getLimit()}
