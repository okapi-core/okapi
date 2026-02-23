package org.okapi.web.service.context;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OrgMemberContext {
    String userId;
    String orgId;
}
