package org.okapi.rest.org;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CreateOrgMemberRequest {
  private String orgId;
  private String email;
  @Setter private boolean admin;
}
