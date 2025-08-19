package org.okapi.metrics.search;

/**
 * Grammar
 * SimpleExpr = KeyExpr | ValueExpr
 * KeyExpr = Tag [*]{1}
 * Tag = [literal]+
 * Pattern = [literal]+ '*'
 * Value = [literal]+
 * ValueExpr = '(' Tag ',' Value ')'
 * KeyExpr = Tag '=' Pattern
 * ValueExpr = [literal]+
 * FollowUp = Condition
 * Tag matches(pattern) and matches() notMatches()
 * KeyValue() matches
 */
public class QueryParser {
    public record TagMatcher(String pattern){}
    public record ValueMatcher(String tag, String pattern){}
    // tag contains matcher
    // value contains or equals matcher
    // OR separator -> (tag = abc* OR (key, value) AND (key, value)) && (tag = abc* OR (key, value))
}
