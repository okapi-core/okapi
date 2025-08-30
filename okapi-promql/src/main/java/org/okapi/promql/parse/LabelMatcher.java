package org.okapi.promql.parse;

public record LabelMatcher(String name, LabelOp op, String value) {}