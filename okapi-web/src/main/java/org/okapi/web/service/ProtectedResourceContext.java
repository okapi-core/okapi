/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.service;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.exceptions.BadRequestException;

@AllArgsConstructor
public class ProtectedResourceContext {
  @Getter String token;
  String resourceId;
  String versionId;

  public static ProtectedResourceContext of(String token) {
    return new ProtectedResourceContext(token, null, null);
  }

  public static ProtectedResourceContext of(String token, String resourceId) {
    return new ProtectedResourceContext(token, resourceId, null);
  }

  public static ProtectedResourceContext of(String token, String resourceId, String versionId) {
    return new ProtectedResourceContext(token, resourceId, versionId);
  }

  public Optional<String> getResourceId() {
    return Optional.ofNullable(resourceId);
  }

  public String getResourceIdOrThrow() throws BadRequestException {
    if (resourceId == null) {
      throw new BadRequestException("Resource ID is required");
    }
    return resourceId;
  }

  public Optional<String> getVersionId() {
    return Optional.ofNullable(versionId);
  }

  public String getVersionIdOrThrow() throws BadRequestException {
    if (versionId == null) {
      throw new BadRequestException("Version ID is required");
    }
    return versionId;
  }
}
