/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.storage.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.okapi.io.OkapiIo;
import org.okapi.io.StreamReadingException;

public class OkapiIoTests {
  @ParameterizedTest
  @ValueSource(
      strings = {"Hello, World!", "Okapi Io Tests", "1234567890", "Special characters !@#$%^&*()"})
  public void testOkapiIo(String target) throws IOException, StreamReadingException {
    var os = new ByteArrayOutputStream(target.length());
    OkapiIo.writeString(os, target);

    var read = os.toByteArray();
    var bis = new ByteArrayInputStream(read);
    var restored = OkapiIo.readString(bis);
    assert restored.equals(target) : "Restored string does not match original";
  }
}
