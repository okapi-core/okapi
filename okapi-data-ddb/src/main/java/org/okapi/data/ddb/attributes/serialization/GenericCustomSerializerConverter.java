package org.okapi.data.ddb.attributes.serialization;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public abstract class GenericCustomSerializerConverter<T> implements AttributeConverter<T> {
  public abstract CustomSerializer<T> getSerializer();

  @Override
  public AttributeValue transformFrom(T t) {
    var str = getSerializer().serialize(t);
    return AttributeValue.builder().s(str).build();
  }

  @Override
  public T transformTo(AttributeValue attributeValue) {
    var str = attributeValue.s();
    return getSerializer().deserialize(str);
  }

  @Override
  public abstract EnhancedType<T> type();

  @Override
  public AttributeValueType attributeValueType() {
    return AttributeValueType.S;
  }
}
