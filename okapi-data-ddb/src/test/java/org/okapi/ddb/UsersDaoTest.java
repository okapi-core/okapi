/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.ddb;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.okapi.data.CreateDynamoDBTables;
import org.okapi.data.dao.UsersDao;
import org.okapi.data.ddb.dao.UsersDaoImpl;
import org.okapi.data.exceptions.UserAlreadyExistsException;
import org.okapi.testutils.OkapiTestUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

@Slf4j
@Execution(ExecutionMode.CONCURRENT)
public class UsersDaoTest {

  UsersDao usersDao;

  @BeforeEach
  public void hydrate() throws UserAlreadyExistsException {
    CreateDynamoDBTables.createTables(OkapiTestUtils.getLocalStackDynamoDbClient());
    usersDao =
        new UsersDaoImpl(
            DynamoDbEnhancedClient.builder()
                .dynamoDbClient(OkapiTestUtils.getLocalStackDynamoDbClient())
                .build());
    try {
      var created =
          usersDao.createIfNotExists("First", "Last", "first.last@gmail.com", "VerySecurePassword");
    } catch (UserAlreadyExistsException e) {
      log.error("Got exception:", e);
    }
  }

  @Test
  public void testUserCreated() throws UserAlreadyExistsException {
    var withEmail = usersDao.getWithEmail("first.last@gmail.com");
    Assertions.assertTrue(withEmail.isPresent());
    var allUsers = Lists.newArrayList(usersDao.listAllUsers());
    assertTrue(allUsers.stream().anyMatch(u -> u.getEmail().equals("first.last@gmail.com")));
  }

  @Test
  public void testCreatingDuplicateUserFails() {
    assertThrows(
        UserAlreadyExistsException.class,
        () ->
            usersDao.createIfNotExists(
                "First", "Last", "first.last@gmail.com", "VerySecurePassword"));
  }

  @Test
  public void testCreatingUserWithoutEmailFails() {
    assertThrows(
        Exception.class,
        () ->
            usersDao.createIfNotExists(
                "First", "Last", "first.last@gmail.com", "VerySecurePassword"));
  }

  @Test
  public void testFailsWithoutFirstname() {
    assertThrows(
        Exception.class,
        () ->
            usersDao.createIfNotExists(null, "Last", "first.last@gmail.com", "VerySecurePassword"));
  }

  @Test
  public void testFailsWithoutLastName() {
    assertThrows(
        Exception.class,
        () ->
            usersDao.createIfNotExists(
                "First", null, "first.last@gmail.com", "VerySecurePassword"));
  }

  @Test
  public void testFailsWithoutPassword() {
    assertThrows(
        Exception.class,
        () -> usersDao.createIfNotExists("First", null, "first.last@gmail.com", null));
  }
}
