package org.okapi.rest.org;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DeleteOrgMemberRequest {
  private String orgId;
  private String email;
}
