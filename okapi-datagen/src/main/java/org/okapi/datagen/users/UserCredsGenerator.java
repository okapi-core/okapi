/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.datagen.users;

import java.util.ArrayList;
import java.util.List;
import net.datafaker.Faker;
import org.okapi.web.dtos.auth.CreateUserRequest;

public class UserCredsGenerator {
  public static List<CreateUserRequest> createUsers(int total) {
    var faker = new Faker();
    var reqs = new ArrayList<CreateUserRequest>();
    for (int i = 0; i < total; i++) {
      var fname = faker.name().firstName();
      var second = faker.name().lastName();
      var email = fname + "." + second + "@acme.org";
      var pw = fname + "@" + second;
      var request =
          CreateUserRequest.builder()
              .firstName(fname)
              .email(email)
              .lastName(second)
              .password(pw)
              .build();
      reqs.add(request);
    }

    return reqs;
  }
}
