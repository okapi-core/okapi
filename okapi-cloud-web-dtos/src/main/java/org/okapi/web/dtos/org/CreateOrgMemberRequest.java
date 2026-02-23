package org.okapi.web.dtos.org;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CreateOrgMemberRequest {
  @NotNull
  private String orgId;
  @NotNull
  private String email;
  @Setter private boolean admin;
}
