@param org.okapi.traces.ch.ChSpanNumericalAttributeSummaryTemplate data

SELECT
    '${data.getAttribute()}' AS attribute_name,
    ${data.getBucketStartExpr()} AS bucket_start_ms,
    ${data.getAggClause()} AS value
FROM ${data.getTable()}
WHERE 1=1
    AND ${data.getPresenceClause()}
@if(data.getWhereClause() != null && !data.getWhereClause().isBlank())
    AND ${data.getWhereClause()}
@endif
GROUP BY bucket_start_ms
ORDER BY bucket_start_ms
