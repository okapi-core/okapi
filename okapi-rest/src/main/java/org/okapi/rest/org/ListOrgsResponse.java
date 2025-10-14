package org.okapi.rest.org;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.okapi.rest.users.GetOrgUserView;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ListOrgsResponse {
  private List<GetOrgUserView> orgs;
}
