package org.okapi.ddb;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.data.dao.UsersDao;
import org.okapi.data.exceptions.UserAlreadyExistsException;
import org.okapi.data.factory.ResourceFactory;

@Slf4j
public class UsersDaoTest {

  ResourceFactory resourceFactory;
  UsersDao usersDao;

  @BeforeEach
  public void hydrate() throws UserAlreadyExistsException {
    resourceFactory = new ResourceFactory();
    usersDao = resourceFactory.usersDao();
    try {
      var created = usersDao.create("First", "Last", "first.last@gmail.com", "VerySecurePassword");
    } catch (UserAlreadyExistsException e) {
      log.error("Got exception:", e);
    }
  }

  @Test
  public void testUserCreated() throws UserAlreadyExistsException {
    var withEmail = usersDao.getWithEmail("first.last@gmail.com");
    Assertions.assertTrue(withEmail.isPresent());
    var allUsers = Lists.newArrayList(usersDao.list());
    assertTrue(allUsers.stream().anyMatch(u -> u.getEmail().equals("first.last@gmail.com")));
  }

  @Test
  public void testCreatingDuplicateUserFails() {
    assertThrows(
        UserAlreadyExistsException.class,
        () -> usersDao.create("First", "Last", "first.last@gmail.com", "VerySecurePassword"));
  }

  @Test
  public void testCreatingUserWithoutEmailFails() {
    assertThrows(
        Exception.class,
        () -> usersDao.create("First", "Last", "first.last@gmail.com", "VerySecurePassword"));
  }

  @Test
  public void testFailsWithoutFirstname() {
    assertThrows(
        Exception.class,
        () -> usersDao.create(null, "Last", "first.last@gmail.com", "VerySecurePassword"));
  }

  @Test
  public void testFailsWithoutLastName() {
    assertThrows(
        Exception.class,
        () -> usersDao.create("First", null, "first.last@gmail.com", "VerySecurePassword"));
  }

  @Test
  public void testFailsWithoutPassword() {
    assertThrows(
        Exception.class, () -> usersDao.create("First", null, "first.last@gmail.com", null));
  }
}
