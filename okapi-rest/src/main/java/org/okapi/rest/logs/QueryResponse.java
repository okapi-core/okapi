package org.okapi.rest.logs;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class QueryResponse {
  public List<LogView> items;
}
