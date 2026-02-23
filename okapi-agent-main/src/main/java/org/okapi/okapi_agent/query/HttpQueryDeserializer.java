package org.okapi.okapi_agent.query;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.okapi.agent.dto.AgentQueryRecords;
import org.okapi.agent.dto.HTTP_METHOD;
import org.okapi.agent.dto.QuerySpec;

@AllArgsConstructor
public class HttpQueryDeserializer {
  HttpHeaderLineDeserializer headerDeserializer;

  public record DeserializationResult(
      AgentQueryRecords.HttpQuery query, boolean success, String errorMessage) {}

  public static HttpQueryDeserializer create() {
    return new HttpQueryDeserializer(HttpHeaderLineDeserializer.create());
  }

  public DeserializationResult readQuery(QuerySpec spec) {
    if (spec == null) {
      return new DeserializationResult(null, false, "QuerySpec is null");
    }
    var query = spec.serializedQuery();
    int bodyIndex = query.indexOf("\r\n\r\n");
    if (bodyIndex == -1) {
      throw new IllegalArgumentException(
          "Invalid HTTP query format: missing header/body separator");
    }

    String head = query.substring(0, bodyIndex);
    String body = query.substring(bodyIndex + 4).trim(); // may be empty

    String[] lines = head.split("\r\n");
    if (lines.length == 0 || lines[0].isEmpty()) {
      throw new IllegalArgumentException("Invalid HTTP query format: missing request line");
    }

    // request line
    String[] requestLine = lines[0].split(" ", 3);
    if (requestLine.length < 3) {
      throw new IllegalArgumentException("Invalid HTTP request line: " + lines[0]);
    }

    HTTP_METHOD method = HTTP_METHOD.valueOf(requestLine[0]);
    String path = requestLine[1];

    var allValidHeaders =
        Arrays.stream(lines)
            .skip(1)
            .filter(line -> !line.isEmpty())
            .allMatch(line -> headerDeserializer.deserializeHeaderLine(line).isPresent());
    if (!allValidHeaders) {
      return new DeserializationResult(null, false, "Invalid HTTP header format");
    }
    // headers
    Map<String, String> headers =
        Arrays.stream(lines)
            .skip(1)
            .filter(line -> !line.isEmpty())
            .map(headerDeserializer::deserializeHeaderLine)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(h -> h[0], h -> h[1], (oldVal, newVal) -> newVal));

    return new HttpQueryDeserializer.DeserializationResult(
        new AgentQueryRecords.HttpQuery(method, path, headers, body), true, null);
  }
}
