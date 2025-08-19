package org.okapi.data.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class TeamMemberDto {
  private String teamId;
  private String userId;
}
