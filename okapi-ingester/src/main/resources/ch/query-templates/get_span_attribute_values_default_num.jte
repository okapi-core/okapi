@param org.okapi.traces.ch.ChSpanAttributeValuesDefaultTemplate data

SELECT
  avg(${data.getAttributeName()}) AS avg,
  quantile(0.25)(${data.getAttributeName()}) AS p25,
  quantile(0.50)(${data.getAttributeName()}) AS p50,
  quantile(0.75)(${data.getAttributeName()}) AS p75,
  quantile(0.90)(${data.getAttributeName()}) AS p90
FROM ${data.getTable()}
WHERE 1=1
@if(data.getTsStartNs() != null)
  AND ts_start_ns >= ${data.getTsStartNs()}
@endif
@if(data.getTsEndNs() != null)
  AND ts_end_ns <= ${data.getTsEndNs()}
@endif
AND isNotNull(${data.getAttributeName()})
