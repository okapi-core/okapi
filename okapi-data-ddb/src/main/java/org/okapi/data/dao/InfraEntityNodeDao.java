/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dao;

import java.util.List;
import java.util.Optional;
import org.okapi.data.ddb.attributes.DEP_TYPE;
import org.okapi.data.ddb.attributes.InfraEntityId;
import org.okapi.data.ddb.attributes.InfraNodeOutgoingEdge;
import org.okapi.data.dto.InfraEntityNodeDdb;
import org.okapi.data.exceptions.EntityDoesNotExistException;

public interface InfraEntityNodeDao {
  void createNode(InfraEntityNodeDdb nodeDdb);

  <T> void updateNodeAttributes(InfraEntityId infraEntityId, T attributes, Class<T> clazz);

  void deleteNode(InfraEntityId infraEntityId);

  void addOutgoingEdge(InfraEntityId entityId, InfraNodeOutgoingEdge outgoingEdge)
      throws EntityDoesNotExistException;

  void removeEdge(InfraEntityId entityId, InfraEntityId targetNodeId);

  List<InfraNodeOutgoingEdge> getEdgesByType(InfraEntityId entityId, DEP_TYPE depType);

  List<InfraNodeOutgoingEdge> getAllOutgoingEdges(InfraEntityId entityId);

  Optional<InfraEntityNodeDdb> getNode(InfraEntityId infraEntityId);
}
