/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.query;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpHeaderLineDeserializerTests {

  HttpHeaderLineDeserializer deserializer = HttpHeaderLineDeserializer.create();

  @Test
  public void deserializeHeaderLine_positiveSamplesAreTrimmedAndParsed() {
    List<String> samples =
        List.of("Header: Value", "Header:Value", " Header : Value ", "Header: Value ");

    for (var sample : samples) {
      Optional<String[]> parsed = deserializer.deserializeHeaderLine(sample);
      Assertions.assertTrue(parsed.isPresent(), "Expected header to parse for sample: " + sample);
      Assertions.assertEquals("Header", parsed.get()[0], "Header key should be preserved");
      Assertions.assertEquals("Value", parsed.get()[1], "Header value should be trimmed");
    }
  }

  @Test
  public void deserializeHeaderLine_negativeSamplesReturnEmptyOptional() {
    List<String> samples = List.of("Header = value", "Header, value", "NoColonHere");

    for (var sample : samples) {
      Optional<String[]> parsed = deserializer.deserializeHeaderLine(sample);
      Assertions.assertTrue(parsed.isEmpty(), "Expected parse failure for sample: " + sample);
    }
  }

  @Test
  public void deserializeHeaderLine_allowsEmptyValueButTrimsWhitespace() {
    Optional<String[]> parsed = deserializer.deserializeHeaderLine("Header:   ");
    Assertions.assertTrue(parsed.isPresent());
    Assertions.assertEquals("Header", parsed.get()[0]);
    Assertions.assertEquals("", parsed.get()[1], "Value should be empty string after trim");
  }

  @Test
  public void deserializeHeaderLine_emptyStringFailsParsing() {
    Assertions.assertTrue(deserializer.deserializeHeaderLine("").isEmpty());
  }
}
