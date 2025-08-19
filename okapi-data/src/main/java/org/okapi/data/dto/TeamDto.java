package org.okapi.data.dto;

import java.time.Instant;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class TeamDto {
  private String teamId;
  private String orgId;
  private String teamName;
  private String description;
  Instant createdAt;
}
