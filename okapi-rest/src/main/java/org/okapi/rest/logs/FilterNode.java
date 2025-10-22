package org.okapi.rest.logs;

import lombok.Data;

@Data
public class FilterNode {
    public String kind; // LEVEL, TRACE, REGEX, AND, OR
    public String regex;
    public String traceId;
    public Integer levelCode;
    public FilterNode left;
    public FilterNode right;
}
