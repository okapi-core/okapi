package org.okapi.web.service.dashboards.hints;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.okapi.rest.TimeInterval;
import org.okapi.rest.search.GetMetricNameHints;
import org.okapi.rest.search.GetSvcHintsRequest;
import org.okapi.rest.search.GetTagValueHintsRequest;
import org.okapi.web.dtos.dashboards.vars.DASH_VAR_TYPE;
import org.okapi.web.dtos.dashboards.vars.GetVarHintsRequest;
import org.okapi.web.dtos.dashboards.vars.VarHintsResponse;
import org.okapi.web.service.access.OrgMemberChecker;
import org.okapi.web.service.client.IngesterClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VariableHintsService {
  final OrgMemberChecker memberChecker;
  final IngesterClient ingesterClient;

  public VarHintsResponse getVarHints(String tempToken, GetVarHintsRequest request) {
    memberChecker.checkUserIsOrgMember(tempToken);
    var queryInterval = buildConstraint(request);
    if (request.getVarType() == DASH_VAR_TYPE.SVC) {
      var hintsRequest = GetSvcHintsRequest.builder().svcPrefix("").interval(queryInterval).build();
      var svcList = ingesterClient.getSvcHints(hintsRequest);
      return VarHintsResponse.builder().suggestions(svcList.getSvcHints()).build();
    } else if (request.getVarType() == DASH_VAR_TYPE.METRIC) {
      var hintsRequest = GetMetricNameHints.builder().interval(queryInterval).build();
      var hints = ingesterClient.getMetricHints(hintsRequest);
      return VarHintsResponse.builder().suggestions(hints.getMetricHints()).build();
    } else {
      var hintsRequest =
          GetTagValueHintsRequest.builder().tag(request.getTag()).interval(queryInterval).build();
      var values = ingesterClient.getTagValueHints(hintsRequest);
      return VarHintsResponse.builder()
          .suggestions(values.getTagValueHints().getCandidates())
          .build();
    }
  }

  public static TimeInterval buildConstraint(GetVarHintsRequest request) {
    if (request.getConstraint() == null) {
      var end = System.currentTimeMillis();
      var st = end - Duration.of(15, ChronoUnit.MINUTES).toMillis();
      return TimeInterval.builder().start(st).end(end).build();
    } else {
      return TimeInterval.builder()
          .start(request.getConstraint().getStart())
          .end(request.getConstraint().getEnd())
          .build();
    }
  }
}
