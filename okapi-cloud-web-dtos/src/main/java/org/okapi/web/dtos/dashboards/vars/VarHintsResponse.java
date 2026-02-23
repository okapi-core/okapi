package org.okapi.web.dtos.dashboards.vars;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.util.List;

@Value
@ToString
@Builder
public class VarHintsResponse {
    List<String> suggestions;
}
