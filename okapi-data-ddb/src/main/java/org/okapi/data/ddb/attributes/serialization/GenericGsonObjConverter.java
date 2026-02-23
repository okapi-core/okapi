/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes.serialization;

import org.okapi.data.dto.GsonSingleton;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public abstract class GenericGsonObjConverter<T> implements AttributeConverter<T> {
  @Override
  public AttributeValue transformFrom(T t) {
    var str = GsonSingleton.SINGLETON.toJson(t);
    return AttributeValue.builder().s(str).build();
  }

  public abstract Class<T> getClazz();

  @Override
  public T transformTo(AttributeValue attributeValue) {
    return GsonSingleton.SINGLETON.fromJson(attributeValue.s(), getClazz());
  }

  @Override
  public abstract EnhancedType<T> type();

  @Override
  public AttributeValueType attributeValueType() {
    return AttributeValueType.S;
  }
}
