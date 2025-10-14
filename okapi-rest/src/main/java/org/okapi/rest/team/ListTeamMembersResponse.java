package org.okapi.rest.team;

import java.util.List;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ListTeamMembersResponse {
  private String teamId;
  List<String> admins;
  List<String> writers;
  List<String> readers;
}
