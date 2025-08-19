package org.okapi.data.dao;

import org.okapi.data.dto.UserDto;
import org.okapi.data.dto.UserRoleRelationDto;
import org.okapi.data.exceptions.UserAlreadyExistsException;
import java.util.Iterator;
import java.util.Optional;

public interface UsersDao {
  Optional<UserDto> get(String userId);

  Optional<UserDto> getWithEmail(String email);

  UserDto create(String firstName, String lastName, String email, String password)
      throws UserAlreadyExistsException;

  Iterator<UserDto> list();

  Iterator<UserRoleRelationDto> listRolesByUserId(String userId);

  Optional<UserRoleRelationDto> checkRole(String userId, String role);

  Iterator<String> listUsersWithRole(String role);

  UserRoleRelationDto grantRole(String userId, String role);

  void revokeRole(String userId, String role);
}
