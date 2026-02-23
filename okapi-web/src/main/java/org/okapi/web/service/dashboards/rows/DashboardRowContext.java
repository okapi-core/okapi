package org.okapi.web.service.dashboards.rows;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.web.service.context.OrgMemberContext;

@AllArgsConstructor
@Getter
public class DashboardRowContext {
    OrgMemberContext orgMember;
    String rowFqId;
}
