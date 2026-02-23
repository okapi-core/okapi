/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.connection;

import java.util.Map;

public record HttpConnectionDetails(Map<String, String> headers) {}
