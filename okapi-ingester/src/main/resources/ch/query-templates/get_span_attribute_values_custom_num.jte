@param org.okapi.traces.ch.ChSpanAttributeValuesCustomTemplate data

SELECT
  avg(${data.getValueExpr()}) AS avg,
  quantile(0.25)(${data.getValueExpr()}) AS p25,
  quantile(0.50)(${data.getValueExpr()}) AS p50,
  quantile(0.75)(${data.getValueExpr()}) AS p75,
  quantile(0.90)(${data.getValueExpr()}) AS p90
FROM ${data.getTable()}
WHERE 1=1
@if(data.getTsStartNs() != null)
  AND ts_start_ns >= ${data.getTsStartNs()}
@endif
@if(data.getTsEndNs() != null)
  AND ts_end_ns <= ${data.getTsEndNs()}
@endif
AND isNotNull(${data.getValueExpr()})
