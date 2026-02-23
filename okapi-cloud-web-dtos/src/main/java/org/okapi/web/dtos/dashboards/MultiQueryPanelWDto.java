package org.okapi.web.dtos.dashboards;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.okapi.web.dtos.constraints.TimeConstraint;
import org.okapi.web.dtos.dashboards.vars.VarsContext;

@AllArgsConstructor
@Getter
@Builder
public class MultiQueryPanelWDto {
  TimeConstraint timeConstraint;
  VarsContext varsContext;
  List<PanelQueryConfigWDto> queries;
}
