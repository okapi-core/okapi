package org.okapi.rest.logs;

import lombok.Data;

import java.util.List;

@Data
public class QueryResponse {
    public List<LogView> items;
    public String nextPageToken;
}
