package org.okapi.metrics.singletons;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.okapi.metrics.common.pojo.Node;

public class AbstractSingletonFactory {
  Map<String, Object> singletons;
  Map<String, Node> registeredNodes;

  public AbstractSingletonFactory() {
    this.singletons = new HashMap<>();
    registeredNodes = new HashMap<>();
  }

  public <T> T makeSingleton(Node node, Class<T> clazz, Supplier<T> objSupplier) {
    if (!registeredNodes.containsKey(node.id())) {
      registeredNodes.put(node.id(), node);
    }
    var key = node.id() + "/" + clazz.getSimpleName();
    if (singletons.containsKey(key)) {
      return (T) singletons.get(key);
    } else {
      singletons.put(key, objSupplier.get());
      return (T) singletons.get(key);
    }
  }

  public <T> T makeSingleton(Class<T> clazz, Supplier<T> objSupplier) {
    var key = "/" + clazz.getSimpleName();
    if (singletons.containsKey(key)) {
      return (T) singletons.get(key);
    } else {
      singletons.put(key, objSupplier.get());
      return (T) singletons.get(key);
    }
  }
}
