/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.okapi_agent.query;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.okapi.agent.dto.HTTP_METHOD;
import org.okapi.agent.dto.QuerySpec;
import org.okapi.okapi_agent.query.HttpQueryDeserializer.DeserializationResult;

public class HttpQueryDeserializerTests {

  HttpQueryDeserializer deserializer = HttpQueryDeserializer.create();

  @Test
  public void readQuery_parsesPositiveSamples() {
    var samples =
        List.of(
            new ExpectedSample(
                """
                GET /alpha HTTP/1.1
                Header: Value
                X-Test: 123

                """,
                HTTP_METHOD.GET,
                "/alpha",
                Map.of("Header", "Value", "X-Test", "123"),
                ""),
            new ExpectedSample(
                """
                POST /payload HTTP/1.1
                Content-Type: text/plain
                Header : Value

                payload-body
                """,
                HTTP_METHOD.POST,
                "/payload",
                Map.of("Content-Type", "text/plain", "Header", "Value"),
                "payload-body"),
            new ExpectedSample(
                """
                GET /dup HTTP/1.1
                X-Dup: first
                X-Dup: second

                """,
                HTTP_METHOD.GET,
                "/dup",
                Map.of("X-Dup", "second"), // ensure last wins
                ""));

    for (int i = 0; i < samples.size(); i++) {
      var sample = samples.get(i);
      DeserializationResult result =
          deserializer.readQuery(new QuerySpec(toCrLf(sample.payload())));
      Assertions.assertTrue(result.success(), "Expected success for sample index " + i);
      Assertions.assertNotNull(result.query());

      var query = result.query();
      Assertions.assertEquals(sample.method(), query.method(), "HTTP method mismatch");
      Assertions.assertEquals(sample.path(), query.path(), "Path mismatch");
      Assertions.assertEquals(
          sample.headers(), query.requestHeaders(), "Header map mismatch for sample " + i);
      Assertions.assertEquals(sample.body(), query.requestBody(), "Body mismatch for sample " + i);
    }
  }

  @Test
  public void readQuery_handlesNegativeSamples() {
    var missingSeparator =
        """
        GET /no-separator HTTP/1.1
        Header: value
        """;
    var missingRequestLine =
        """

        Header: value

        """;
    var malformedRequestLine =
        """
        GET /onlytwo
        Header: value

        """;
    var invalidHeaderLine =
        """
        GET /bad-header HTTP/1.1
        Header = value

        """;
    var unsupportedMethod =
        """
        FETCH /unknown HTTP/1.1
        Header: value

        """;

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> deserializer.readQuery(new QuerySpec(toCrLf(missingSeparator))),
        "Missing separator should throw");

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> deserializer.readQuery(new QuerySpec(toCrLf(missingRequestLine))),
        "Missing request line should throw");

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> deserializer.readQuery(new QuerySpec(toCrLf(malformedRequestLine))),
        "Malformed request line should throw");

    var result = deserializer.readQuery(new QuerySpec(toCrLf(invalidHeaderLine)));
    Assertions.assertFalse(result.success());
    Assertions.assertNull(result.query());
    Assertions.assertTrue(
        result.errorMessage().contains("header"), "Should include header error message");

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> deserializer.readQuery(new QuerySpec(toCrLf(unsupportedMethod))),
        "Unsupported HTTP method should throw");
  }

  private String toCrLf(String block) {
    var normalized = block.replace("\r\n", "\n").replace("\r", "\n");
    return normalized.replace("\n", "\r\n");
  }

  private record ExpectedSample(
      String payload, HTTP_METHOD method, String path, Map<String, String> headers, String body) {}
}
