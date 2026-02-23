/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes.serialization;

import org.okapi.data.ddb.attributes.TagsList;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class TagsListConverterGson extends GenericGsonObjConverter<TagsList> {
  @Override
  public Class<TagsList> getClazz() {
    return TagsList.class;
  }

  @Override
  public EnhancedType<TagsList> type() {
    return EnhancedType.of(TagsList.class);
  }
}
