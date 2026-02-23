/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.agent.dto;

import lombok.Builder;

@Builder
public record QuerySpec(String serializedQuery) {}
