@param org.okapi.traces.ch.ChSpansQueryTemplate data

SELECT
    ts_start_ns,
    ts_end_ns,
    trace_id,
    span_id,
    parent_span_id,
    kind,
    kind_string,
    service_name,
    service_peer_name,
    http_method,
    http_status_code,
    http_request_size,
    http_response_size,
    http_origin,
    http_host,
    server_address,
    server_port,
    client_address,
    client_port,
    source_address,
    source_port,
    network_protocol_type,
    network_protocol_version,
    db_system_name,
    db_collection_name,
    db_namespace,
    db_operation_name,
    db_response_status_code,
    db_query_text,
    db_query_summary,
    db_stored_procedure_name,
    db_response_returned_rows,
    rpc_method,
    rpc_method_original,
    rpc_response_status_code,
    attribs_str_0,
    attribs_str_1,
    attribs_str_2,
    attribs_str_3,
    attribs_str_4,
    attribs_str_5,
    attribs_str_6,
    attribs_str_7,
    attribs_str_8,
    attribs_str_9,
    attribs_number_0,
    attribs_number_1,
    attribs_number_2,
    attribs_number_3,
    attribs_number_4,
    attribs_number_5,
    attribs_number_6,
    attribs_number_7,
    attribs_number_8,
    attribs_number_9
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
@if(data.getHttpStatusCode() != null)
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
ORDER BY ts_start_ns DESC
LIMIT ${data.getLimit()}
