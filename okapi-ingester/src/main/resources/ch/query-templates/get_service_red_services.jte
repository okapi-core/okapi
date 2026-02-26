@param org.okapi.traces.ch.template.ChServiceRedServicesTemplate data

SELECT DISTINCT service_name
FROM ${data.getTable()}
WHERE 1=1
@if(data.getTimestampFilter() != null)
  AND ts_start_nanos >= ${data.getTimestampFilter().getTsStartNanos()}
  AND ts_end_nanos <= ${data.getTimestampFilter().getTsEndNanos()}
@endif
  AND service_name != ''
ORDER BY service_name
