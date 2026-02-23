package org.okapi.web.investigation.output;

import com.google.gson.Gson;
import java.util.function.Supplier;
import org.okapi.json.JsonExtractor;

public class GsonSubnodeConverter {
  public static <T> T convertObj(
      Gson gson,
      Class<T> clazz,
      String[] path,
      JsonExtractor extractor,
      Supplier<RuntimeException> exceptionSupplier) {
    var object = extractor.getObject(path);
    if (object.isEmpty()) {
      throw exceptionSupplier.get();
    }
    return gson.fromJson(object.get(), clazz);
  }
}
