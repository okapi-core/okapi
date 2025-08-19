package com.okapi.rest.org;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UpdateTeamRequest {
    private String name; // The name of the team
    private String description; // Optional, can be used to provide a description of the team
}
