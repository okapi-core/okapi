package org.okapi.data.ddb.attributes;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class MultiQueryPanelConfig {
  List<PanelQueryConfig> queryConfigs;
}
