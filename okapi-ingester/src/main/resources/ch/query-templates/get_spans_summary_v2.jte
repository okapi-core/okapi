@param org.okapi.traces.ch.ChSpansQueryTemplate data

SELECT COUNT() as result_count
FROM ${data.getTable()}
WHERE 1=1
@if(data.getTraceId() != null && !data.getTraceId().isEmpty())
    AND trace_id = '${data.getTraceId()}'
@endif
@if(data.getSpanId() != null && !data.getSpanId().isEmpty())
    AND span_id = '${data.getSpanId()}'
@endif
@if(data.getKind() != null && !data.getKind().isEmpty())
    AND kind = '${data.getKind()}'
@endif
@if(data.getServiceName() != null && !data.getServiceName().isEmpty())
    AND service_name = '${data.getServiceName()}'
@endif
@if(data.getServicePeerName() != null && !data.getServicePeerName().isEmpty())
    AND service_peer_name = '${data.getServicePeerName()}'
@endif
@if(data.getHttpMethod() != null && !data.getHttpMethod().isEmpty())
    AND http_method = '${data.getHttpMethod()}'
@endif
@if(data.getHttpStatusCode() != null && data.getHttpStatusCode() >= 100)
    AND http_status_code = ${data.getHttpStatusCode()}
@endif
@if(data.getHttpOrigin() != null && !data.getHttpOrigin().isEmpty())
    AND http_origin = '${data.getHttpOrigin()}'
@endif
@if(data.getHttpHost() != null && !data.getHttpHost().isEmpty())
    AND http_host = '${data.getHttpHost()}'
@endif
@if(data.getDbSystem() != null && !data.getDbSystem().isEmpty())
    AND db_system_name = '${data.getDbSystem()}'
@endif
@if(data.getDbCollection() != null && !data.getDbCollection().isEmpty())
    AND db_collection_name = '${data.getDbCollection()}'
@endif
@if(data.getDbNamespace() != null && !data.getDbNamespace().isEmpty())
    AND db_namespace = '${data.getDbNamespace()}'
@endif
@if(data.getDbOperation() != null && !data.getDbOperation().isEmpty())
    AND db_operation_name = '${data.getDbOperation()}'
@endif
@if(data.getTsStartNs() != null)
    AND ts_start_ns >= ${data.getTsStartNs()}
@endif
@if(data.getTsEndNs() != null)
    AND ts_end_ns <= ${data.getTsEndNs()}
@endif
@if(data.getDurMinNs() != null)
    AND (ts_end_ns - ts_start_ns) >= ${data.getDurMinNs()}
@endif
@if(data.getDurMaxNs() != null)
    AND (ts_end_ns - ts_start_ns) <= ${data.getDurMaxNs()}
@endif
@if(data.getStringAttributeFilters() != null && !data.getStringAttributeFilters().isEmpty())
    @for(var f : data.getStringAttributeFilters())
        AND attribs_str_${f.getBucket()}['${f.getKey()}'] = '${f.getValue()}'
    @endfor
@endif
@if(data.getNumberAttributeFilters() != null && !data.getNumberAttributeFilters().isEmpty())
    @for(var f : data.getNumberAttributeFilters())
        AND attribs_number_${f.getBucket()}['${f.getKey()}'] = ${f.getValue()}
    @endfor
@endif
