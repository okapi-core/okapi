/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.profiles;

public enum ENV_TYPE {
  PROD("prod"),
  TEST("test"),
  ISO("isolated"),
  INTEG_TEST("integration-test");

  private String env;

  ENV_TYPE(String env) {
    this.env = env;
  }

  public static ENV_TYPE parse(String env) {
    for (ENV_TYPE type : ENV_TYPE.values()) {
      if (type.env.equalsIgnoreCase(env)) {
        return type;
      }
    }
    return null;
  }

  public String getEnv() {
    return env;
  }
}
