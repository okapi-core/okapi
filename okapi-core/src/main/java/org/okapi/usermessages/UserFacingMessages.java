package org.okapi.usermessages;

public class UserFacingMessages { // users related
  public static final String USER_ALREADY_EXISTS =
      "User with email already exists, try logging in instead.";
  public static final String USER_NOT_FOUND = "User not found.";

  // passwords related
  public static final String NO_PASSWORD = "Password must be specified.";
  public static final String WRONG_CREDS = "Wrong email or password.";
  // org CRUD related
  public static final String CANT_REMOVE_SELF = "Cannot remove org creator from the org.";
  public static final String ORG_NOT_FOUND = "Organization not found.";

  // metrics related
  public static final String METRIC_TYPE_MISSING = "Request should specify a metric type.";

  // metrics validation messages
  public static final String GAUGE_PAYLOAD_MISSING = "Gauge payload must be supplied.";
  public static final String GAUGE_TIMESTAMPS_TOO_FAR_APART =
      "Gauge timestamps must be within 24 hours.";

  public static final String HISTO_PAYLOAD_MISSING = "Histogram payload must be supplied.";
  public static final String HISTO_LES_VALUES_REQUIRED =
      "Histogram bounds (les) and values must be supplied.";
  public static final String HISTO_ARRAYS_MUST_BE_EQUAL_LENGTH =
      "Histogram les and values arrays must be the same length.";
  public static final String HISTO_VALUES_MUST_BE_POSITIVE =
      "All histogram values must be positive.";

  public static final String COUNTER_PAYLOAD_MISSING = "Counter payload must be supplied.";
  public static final String COUNTER_TS_REQUIRED = "Counter timestamps must be supplied.";
  public static final String TIME_FILTER_MISSING = "Time filter is missing.";
}
