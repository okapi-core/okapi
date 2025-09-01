package org.okapi.rest.org;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class GetOrgResponse {
  private String orgId;
  private String orgName;
  private List<String> members;
  private List<String> admins;
}
