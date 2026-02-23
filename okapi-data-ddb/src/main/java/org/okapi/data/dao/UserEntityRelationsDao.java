/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dao;

import java.util.List;
import java.util.Optional;
import org.okapi.data.ddb.attributes.EntityRelationId;
import org.okapi.data.dto.UserEntityRelations;

public interface UserEntityRelationsDao {
  Optional<UserEntityRelations> getRelation(String userId, EntityRelationId edgeId);

  List<UserEntityRelations> listUserRelations(String userId);

  void createRelation(UserEntityRelations userEntityRelations);

  void deleteRelation(String userId, EntityRelationId edgeId);
}
