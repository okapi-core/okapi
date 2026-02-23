package org.okapi.web.dtos.logs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LogsViewLogDto {
  String appId;
  Instant lastViewedAt;
}
