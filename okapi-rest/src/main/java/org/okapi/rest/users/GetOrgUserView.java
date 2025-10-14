package org.okapi.rest.users;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class GetOrgUserView {
  String orgName;
  String orgId;
  boolean isCreator;
  List<String> roles;
}
