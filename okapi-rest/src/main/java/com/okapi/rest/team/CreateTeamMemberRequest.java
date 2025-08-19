package com.okapi.rest.team;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeamMemberRequest {
    private String email;
    private boolean reader;
    private boolean writer;
    private boolean admin;
}
