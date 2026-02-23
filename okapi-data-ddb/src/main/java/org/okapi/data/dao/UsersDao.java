/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dao;

import java.util.Iterator;
import java.util.Optional;
import org.okapi.data.dto.UserDtoDdb;
import org.okapi.data.exceptions.UserAlreadyExistsException;

public interface UsersDao {
  Optional<UserDtoDdb> get(String userId);

  Optional<UserDtoDdb> getWithEmail(String email);

  UserDtoDdb createIfNotExists(String firstName, String lastName, String email, String password)
      throws UserAlreadyExistsException;

  Iterator<UserDtoDdb> listAllUsers();

  void update(UserDtoDdb userDtoDdb);
}
