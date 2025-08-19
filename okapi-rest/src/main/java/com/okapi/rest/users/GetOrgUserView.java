package com.okapi.rest.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
@Builder
public class GetOrgUserView {
    String orgName;
    String orgId;
    boolean isCreator;
    List<String> roles;
}
