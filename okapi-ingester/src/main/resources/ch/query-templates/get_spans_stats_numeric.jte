@param org.okapi.traces.ch.ChSpansStatsNumericTemplate data

SELECT
attribute_name,
bucket_start_ms,
value
FROM
(
@for(int i = 0; i < data.getAttributes().size(); i++)
    SELECT
    '${data.getAttributes().get(i)}' AS attribute_name,
    ${data.getBucketStartExpr()} AS bucket_start_ms,
    ${data.getAggClauses().get(data.getAttributes().get(i))} AS value
    FROM ${data.getTable()}
    WHERE 1=1
    @if(data.getRawClauses() != null && !data.getRawClauses().isEmpty())
        @for(var clause : data.getRawClauses())
            AND ${clause}
        @endfor
    @endif
    @if(data.getColFilters() != null && !data.getColFilters().isEmpty())
        @for(var f : data.getColFilters())
            @if(f.getStrValue() != null)
                AND ${f.getColName()} = '${f.getStrValue()}'
            @elseif(f.getIntValue() != null)
                AND ${f.getColName()} = ${f.getIntValue()}
            @elseif(f.getDoubleValue() != null)
                AND ${f.getColName()} = ${f.getDoubleValue()}
            @endif
        @endfor
    @endif
    GROUP BY bucket_start_ms
    ORDER BY bucket_start_ms
    @if(i < data.getAttributes().size() - 1)
        UNION ALL
    @endif
@endfor
)
