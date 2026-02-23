@param org.okapi.traces.ch.ChSpansStatsCountTemplate data

SELECT
    count() AS cnt
FROM ${data.getTable()}
WHERE 1=1
@if(data.getTraceId() != null && !data.getTraceId().isEmpty())
  AND trace_id = '${data.getTraceId()}'
@endif
@if(data.getKind() != null && !data.getKind().isEmpty())
  AND kind = '${data.getKind()}'
@endif
@if(data.getServiceFilter() != null && data.getServiceFilter().getService() != null && !data.getServiceFilter().getService().isEmpty())
  AND service_name = '${data.getServiceFilter().getService()}'
@endif
@if(data.getServiceFilter() != null && data.getServiceFilter().getPeer() != null && !data.getServiceFilter().getPeer().isEmpty())
  AND service_peer_name = '${data.getServiceFilter().getPeer()}'
@endif
@if(data.getHttpFilters() != null && data.getHttpFilters().getHttpMethod() != null && !data.getHttpFilters().getHttpMethod().isEmpty())
  AND http_method = '${data.getHttpFilters().getHttpMethod()}'
@endif
@if(data.getHttpFilters() != null && data.getHttpFilters().getStatusCode() != null)
  AND http_status_code = ${data.getHttpFilters().getStatusCode()}
@endif
@if(data.getHttpFilters() != null && data.getHttpFilters().getOrigin() != null && !data.getHttpFilters().getOrigin().isEmpty())
  AND http_origin = '${data.getHttpFilters().getOrigin()}'
@endif
@if(data.getHttpFilters() != null && data.getHttpFilters().getHost() != null && !data.getHttpFilters().getHost().isEmpty())
  AND http_host = '${data.getHttpFilters().getHost()}'
@endif
@if(data.getDbFilters() != null && data.getDbFilters().getSystem() != null && !data.getDbFilters().getSystem().isEmpty())
  AND db_system_name = '${data.getDbFilters().getSystem()}'
@endif
@if(data.getDbFilters() != null && data.getDbFilters().getCollection() != null && !data.getDbFilters().getCollection().isEmpty())
  AND db_collection_name = '${data.getDbFilters().getCollection()}'
@endif
@if(data.getDbFilters() != null && data.getDbFilters().getNamespace() != null && !data.getDbFilters().getNamespace().isEmpty())
  AND db_namespace = '${data.getDbFilters().getNamespace()}'
@endif
@if(data.getDbFilters() != null && data.getDbFilters().getOperation() != null && !data.getDbFilters().getOperation().isEmpty())
  AND db_operation_name = '${data.getDbFilters().getOperation()}'
@endif
@if(data.getTimestampFilter() != null)
  AND ts_start_ns >= ${data.getTimestampFilter().getTsStartNanos()}
  AND ts_end_ns <= ${data.getTimestampFilter().getTsEndNanos()}
@endif
@if(data.getDurationFilter() != null)
  AND (ts_end_ns - ts_start_ns) >= ${data.getDurationFilter().getDurMinMillis() * 1000000L}
  AND (ts_end_ns - ts_start_ns) <= ${data.getDurationFilter().getDurMaxMillis() * 1000000L}
@endif
@if(data.getStringFilters() != null && !data.getStringFilters().isEmpty())
@for(var f : data.getStringFilters())
  AND attribs_str_${f.getBucket()}['${f.getKey()}'] = '${f.getValue()}'
@endfor
@endif
@if(data.getNumberFilters() != null && !data.getNumberFilters().isEmpty())
@for(var f : data.getNumberFilters())
  AND attribs_number_${f.getBucket()}['${f.getKey()}'] = ${f.getValue()}
@endfor
@endif
