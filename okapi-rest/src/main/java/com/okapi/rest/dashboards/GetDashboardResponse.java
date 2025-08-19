package com.okapi.rest.dashboards;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class GetDashboardResponse {
  String id;
  String definition;
  String note;
  String title;
  Instant created;
  Instant updated;
}
