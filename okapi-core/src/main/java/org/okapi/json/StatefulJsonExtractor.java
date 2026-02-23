package org.okapi.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A Json extractor that parses once and allows repeated lookups without reparsing.
 */
public class StatefulJsonExtractor {
  private final JsonElement root;

  public StatefulJsonExtractor(String json) {
    this.root = JsonParser.parseString(json);
  }

  public StatefulJsonExtractor(JsonElement element) {
    this.root = element;
  }

  public Optional<JsonObject> getObject(Iterable<String> path) {
    return navigate(path)
        .filter(JsonElement::isJsonObject)
        .map(JsonElement::getAsJsonObject);
  }

  public Optional<JsonArray> getArray(Iterable<String> path) {
    return navigate(path).filter(JsonElement::isJsonArray).map(JsonElement::getAsJsonArray);
  }

  public Optional<String> getString(Iterable<String> path) {
    return navigate(path).filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsString);
  }

  public Optional<Long> getLong(Iterable<String> path) {
    return navigate(path).filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsLong);
  }

  public Optional<Double> getDouble(Iterable<String> path) {
    return navigate(path).filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsDouble);
  }

  private Optional<JsonElement> navigate(Iterable<String> path) {
    List<String> keys = new ArrayList<>();
    for (String p : path) {
      keys.add(p);
    }
    if (keys.isEmpty()) {
      return Optional.of(root);
    }
    JsonElement current = root;
    for (int i = 0; i < keys.size(); i++) {
      if (current == null || current.isJsonNull()) {
        return Optional.empty();
      }
      String key = keys.get(i);
      if (!current.isJsonObject()) {
        return Optional.empty();
      }
      JsonObject obj = current.getAsJsonObject();
      if (!obj.has(key)) {
        return Optional.empty();
      }
      current = obj.get(key);
    }
    return Optional.ofNullable(current);
  }
}
