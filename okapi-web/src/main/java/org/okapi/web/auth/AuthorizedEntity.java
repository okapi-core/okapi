/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.auth;

import com.google.common.base.Preconditions;
import java.util.List;
import org.okapi.exceptions.UnAuthorizedException;

public record AuthorizedEntity(String orgId, List<String> permissions) {

  public AuthorizedEntity(String orgId, List<String> permissions) {
    this.orgId = Preconditions.checkNotNull(orgId);
    this.permissions = Preconditions.checkNotNull(permissions);
  }

  public boolean hasPermission(String permission) {
    var isAuthorized = permissions.contains(permission);
    if (!isAuthorized) {
      throw new UnAuthorizedException("Permission " + permission + " is required.");
    }
    return true;
  }

  public boolean hasAnyPermission(List<String> perms) {
    for (String perm : perms) {
      if (permissions.contains(perm)) {
        return true;
      }
    }
    return false;
  }

  public boolean hasAllPermissions(List<String> perms) {
    for (String perm : perms) {
      if (!permissions.contains(perm)) {
        return false;
      }
    }
    return true;
  }
}
