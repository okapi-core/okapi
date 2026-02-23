/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.pojo;

import java.util.Map;

public record MetricsBatch(String name, Map<String, String> tags, Long timestamps, Double value) {}
