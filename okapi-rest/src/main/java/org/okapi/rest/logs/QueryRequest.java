package org.okapi.rest.logs;

import lombok.Data;

@Data
public class QueryRequest {
    public long start;
    public long end;
    public int limit;
    public String pageToken;
    public FilterNode filter;
}