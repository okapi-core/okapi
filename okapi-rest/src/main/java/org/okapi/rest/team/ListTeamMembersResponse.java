package org.okapi.rest.team;

import lombok.*;

import java.util.List;

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
