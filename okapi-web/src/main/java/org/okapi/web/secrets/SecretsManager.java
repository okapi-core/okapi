/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.secrets;

public interface SecretsManager {
  String getHmacKey();

  String getApiKey();
}
