package org.okapi.metrics.query.promql;

import com.google.re2j.Pattern;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import org.okapi.metrics.common.MetricsPathParser;
import org.okapi.promql.parse.LabelMatcher;

public class PathMatchConditions {

  public static boolean pathMatchesConditions(
      MetricsPathParser.MetricsRecord record, List<LabelMatcher> matchers) {
    for (var matcher : matchers) {
      var name = matcher.name();
      var hasLabel = record.tags().containsKey(name);
      if (!hasLabel) return false;
      var value = record.tags().get(name);
      var matchArg = matcher.value();
      BiFunction<String, String, Boolean> matchingFn =
          switch (matcher.op()) {
            case EQ -> PathMatchConditions::isStringEqual;
            case NE -> PathMatchConditions::isStringNotEqual;
            case RE -> PathMatchConditions::isPatternMatch;
            case NRE -> PathMatchConditions::isNotPatternMatch;
          };
      var result = matchingFn.apply(value, matchArg);
      if (!result) return false;
    }
    return true;
  }

  public static boolean isStringEqual(String value, String pattern) {
    return Objects.equals(value, pattern);
  }

  public static boolean isStringNotEqual(String value, String pattern) {
    return !Objects.equals(value, pattern);
  }

  public static boolean isPatternMatch(String value, String pattern) {
    var compiled = Pattern.compile(pattern);
    return compiled.matches(value);
  }

  public static boolean isNotPatternMatch(String value, String pattern) {
    var compiled = Pattern.compile(pattern);
    return !compiled.matches(value);
  }
}
