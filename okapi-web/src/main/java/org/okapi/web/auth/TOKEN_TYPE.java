package org.okapi.web.auth;

import lombok.Getter;

public enum TOKEN_TYPE {
  LOGIN("login"),
  TEMP("temp"),
  BEARER("bearer");

  @Getter final String name;

  TOKEN_TYPE() {
    this.name = this.name().toLowerCase();
  }

  TOKEN_TYPE(String name) {
    this.name = name;
  }
}
