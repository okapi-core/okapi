@param org.okapi.traces.ch.template.ChServiceRedPeersTemplate data

SELECT DISTINCT peer_service_name
FROM ${data.getTable()}
WHERE 1=1
@if(data.getServiceName() != null && !data.getServiceName().isEmpty())
  AND service_name = '${data.getServiceName()}'
@endif
@if(data.getTimestampFilter() != null)
  AND ts_start_nanos >= ${data.getTimestampFilter().getTsStartNanos()}
  AND ts_end_nanos <= ${data.getTimestampFilter().getTsEndNanos()}
@endif
  AND peer_service_name != ''
ORDER BY peer_service_name
