package org.okapi.data.ddb.attributes.serialization;

public interface CustomSerializer<T> {
  String serialize(T obj);

  T deserialize(String str);
}
