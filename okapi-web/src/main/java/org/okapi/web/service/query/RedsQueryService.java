package org.okapi.web.service.query;

import lombok.RequiredArgsConstructor;
import org.okapi.rest.traces.red.ListServicesRequest;
import org.okapi.rest.traces.red.ServiceListResponse;
import org.okapi.rest.traces.red.ServiceRedRequest;
import org.okapi.rest.traces.red.ServiceRedResponse;
import org.okapi.web.service.access.OrgMemberChecker;
import org.okapi.web.service.client.IngesterClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedsQueryService {

  private final IngesterClient ingesterClient;
  private final OrgMemberChecker orgMemberChecker;

  public ServiceListResponse listServices(String token, ListServicesRequest request) {
    orgMemberChecker.checkUserIsOrgMember(token);
    return ingesterClient.getSvcList(request);
  }

  public ServiceRedResponse getServicesReds(String tok, ServiceRedRequest request) {
    orgMemberChecker.checkUserIsOrgMember(tok);
    return ingesterClient.getServiceReds(request);
  }
}
