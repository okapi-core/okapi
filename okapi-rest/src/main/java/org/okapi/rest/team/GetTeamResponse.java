package org.okapi.rest.team;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class GetTeamResponse {
    private String orgId;
    private String teamId;
    private String teamName;
    private String description;
    private Instant createdAt;
    private boolean admin;
    private boolean reader;
    private boolean writer;
}
