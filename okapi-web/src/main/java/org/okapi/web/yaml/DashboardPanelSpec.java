package org.okapi.web.yaml;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class DashboardPanelSpec {
  String id;
  String title;
  String note;
  List<PanelQuerySpec> queries;
}
