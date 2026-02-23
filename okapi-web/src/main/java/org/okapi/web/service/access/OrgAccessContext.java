package org.okapi.web.service.access;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class OrgAccessContext {
    String orgId;
    String userId;
}
