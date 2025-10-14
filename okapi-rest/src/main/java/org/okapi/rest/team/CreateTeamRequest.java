package org.okapi.rest.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class CreateTeamRequest {
  private String name; // The name of the team
  private String description; // Optional, can be used to provide a description of the team
}
