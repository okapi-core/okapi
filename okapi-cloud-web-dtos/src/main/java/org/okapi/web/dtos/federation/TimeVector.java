/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.dtos.federation;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimeVector {
  @NotNull List<Long> timestamps;
  @NotNull List<Float> values;
}
