/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics;

import java.security.SecureRandom;
import java.util.UUID;

public class IdCreator {
  public static final int INT_TOSSES = 20;
  public static final int ROUNDS = 3;

  public static String getTenantId(String orgId, String teamId) {
    return orgId + "_" + teamId;
  }

  public static String createUuidWithoutDashes() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  public static String createRandomString(int len) {
    String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    SecureRandom random = new SecureRandom();
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      sb.append(characters.charAt(random.nextInt(characters.length())));
    }
    return sb.toString();
  }

  public static String increasinglyLargeRandomStrings(int start, ClashDetector clashDetector)
      throws IdCreationFailedException {
    var startLength = start;
    for (int i = 0; i < ROUNDS; i++) {
      for (int j = 0; j < INT_TOSSES; j++) {
        var random = createRandomString(startLength);
        if (!clashDetector.isAClash(random)) return random;
      }
      startLength++;
    }
    throw new IdCreationFailedException();
  }

  public static String generateOrgId(ClashDetector detector) throws IdCreationFailedException {
    return increasinglyLargeRandomStrings(4, detector);
  }

  public static String generateUniqueId(ClashDetector clashDetector)
      throws IdCreationFailedException {
    return increasinglyLargeRandomStrings(4, clashDetector);
  }

  public interface ClashDetector {
    boolean isAClash(String id);
  }
}
