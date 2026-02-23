package org.okapi.web.dtos.federation;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class FederatedQueryRequest {
  @NotNull String sourceId;
  @NotNull String query;
  @NotNull EXPECTED_RESULT expected;
  @NotNull String dedupKey;
  @NotNull String legendFn;
  @NotNull Map<String, Object> queryContext;
}
