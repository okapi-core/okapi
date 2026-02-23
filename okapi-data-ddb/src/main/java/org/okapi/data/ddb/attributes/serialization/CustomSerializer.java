/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes.serialization;

public interface CustomSerializer<T> {
  String serialize(T obj);

  T deserialize(String str);
}
