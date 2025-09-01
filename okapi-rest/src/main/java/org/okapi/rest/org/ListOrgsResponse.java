package org.okapi.rest.org;

import org.okapi.rest.users.GetOrgUserView;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ListOrgsResponse {
  private List<GetOrgUserView> orgs;
}
