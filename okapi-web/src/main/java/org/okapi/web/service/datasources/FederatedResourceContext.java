package org.okapi.web.service.datasources;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.web.service.context.OrgMemberContext;

@AllArgsConstructor
@Getter
public class FederatedResourceContext {
  OrgMemberContext memberContext;
  String sourceId;
}
