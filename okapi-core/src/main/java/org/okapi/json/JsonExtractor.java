package org.okapi.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class JsonExtractor {
  @Getter String json;

  public Optional<JsonObject> getObject(String[] path) {
    return getObject(Arrays.asList(path));
  }

  public Optional<JsonObject> getObject(List<String> path) {
    var obj = JsonParser.parseString(json).getAsJsonObject();
    for (String p : path) {
      if (obj.has(p)) {
        obj = obj.getAsJsonObject(p);
      } else {
        return Optional.empty();
      }
    }
    return Optional.of(obj);
  }

  public Optional<JsonArray> getArray(List<String> path) {
    var parsed = JsonParser.parseString(json).getAsJsonObject();
    for (var subPath : path.subList(0, path.size() - 1)) {
      if (parsed.has(subPath)) {
        parsed = parsed.getAsJsonObject(subPath);
      } else {
        return Optional.empty();
      }
    }
    var lastKey = path.get(path.size() - 1);
    if (parsed.has(lastKey)) {
      return Optional.of(parsed.getAsJsonArray(lastKey));
    }
    return Optional.empty();
  }

  public Optional<Boolean> getAsBoolean(String[] path) {
    var objOpt = getObject(path);
    if (objOpt.isPresent()) {
      var obj = objOpt.get();
      var lastKey = path[path.length - 1];
      if (obj.has(lastKey)) {
        return Optional.of(obj.get(lastKey).getAsBoolean());
      }
    }
    return Optional.empty();
  }

  public Optional<String> getAsString(String[] path) {
    return getAsString(Arrays.asList(path));
  }

  public Optional<String> getAsString(List<String> path) {
    if (path.size() == 1) {
      var obj = JsonParser.parseString(json).getAsJsonObject();
      var lastKey = path.getFirst();
      if (obj.has(lastKey)) {
        return Optional.of(obj.get(lastKey).getAsString());
      } else {
        return Optional.empty();
      }
    }
    var objOpt = getObject(path.subList(0, path.size() - 1));
    if (objOpt.isPresent()) {
      var obj = objOpt.get();
      var lastKey = path.getLast();
      if (obj.has(lastKey)) {
        return Optional.of(obj.get(lastKey).getAsString());
      }
    }
    return Optional.empty();
  }

  public Optional<Double> getAsDouble(String[] path) {
    var objOpt = getObject(path);
    if (objOpt.isPresent()) {
      var obj = objOpt.get();
      var lastKey = path[path.length - 1];
      if (obj.has(lastKey)) {
        return Optional.of(obj.get(lastKey).getAsDouble());
      }
    }
    return Optional.empty();
  }

  public Optional<Integer> getAsInteger(String[] path) {
    var objOpt = getObject(path);
    if (objOpt.isPresent()) {
      var obj = objOpt.get();
      var lastKey = path[path.length - 1];
      if (obj.has(lastKey)) {
        return Optional.of(obj.get(lastKey).getAsInt());
      }
    }
    return Optional.empty();
  }
}
