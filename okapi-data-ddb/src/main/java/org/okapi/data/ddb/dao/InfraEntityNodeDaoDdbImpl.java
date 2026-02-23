/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.dao;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.okapi.data.dao.InfraEntityNodeDao;
import org.okapi.data.ddb.attributes.DEP_TYPE;
import org.okapi.data.ddb.attributes.InfraEntityId;
import org.okapi.data.ddb.attributes.InfraNodeOutgoingEdge;
import org.okapi.data.ddb.attributes.InfraNodeOutgoingEdges;
import org.okapi.data.ddb.attributes.serialization.InfraEntityIdDdbConverter;
import org.okapi.data.dto.GsonSingleton;
import org.okapi.data.dto.InfraEntityNodeDdb;
import org.okapi.data.dto.TablesAndIndexes;
import org.okapi.data.exceptions.EntityDoesNotExistException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * DynamoDB implementation for InfraEntityNodeDao. All operations are O(E) where E is the number of
 * outgoing edges for a node (reads bring the edge list once; updates rewrite the list once). If we
 * needed per-edge atomic mutations without reading the whole list, the current single-item list
 * model would be a domain modeling constraint.
 */
public class InfraEntityNodeDaoDdbImpl implements InfraEntityNodeDao {

  private final DynamoDbTable<InfraEntityNodeDdb> table;
  private final InfraEntityIdDdbConverter keyConverter = new InfraEntityIdDdbConverter();

  @Inject
  public InfraEntityNodeDaoDdbImpl(DynamoDbEnhancedClient enhancedClient) {
    this.table =
        enhancedClient.table(
            TablesAndIndexes.INFRA_ENTITY_NODES_TABLE,
            TableSchema.fromBean(InfraEntityNodeDdb.class));
  }

  @Override
  public void createNode(InfraEntityNodeDdb nodeDdb) {
    if (nodeDdb == null) return;
    // ensure non-null edges container
    if (nodeDdb.getOutgoingEdges() == null) {
      nodeDdb.setOutgoingEdges(new InfraNodeOutgoingEdges(new ArrayList<>()));
    } else if (nodeDdb.getOutgoingEdges().getEdges() == null) {
      nodeDdb.setOutgoingEdges(new InfraNodeOutgoingEdges(new ArrayList<>()));
    }
    table.putItem(nodeDdb);
  }

  @Override
  public <T> void updateNodeAttributes(InfraEntityId infraEntityId, T attributes, Class<T> clazz) {
    var existing = getNode(infraEntityId);
    var json = GsonSingleton.SINGLETON.toJson(attributes);
    if (existing.isPresent()) {
      var item = existing.get();
      item.setAttributes(json);
      table.putItem(item);
    } else {
      // Upsert with empty edges
      var node = new InfraEntityNodeDdb();
      node.setInfraEntityId(infraEntityId);
      node.setAttributes(json);
      node.setOutgoingEdges(new InfraNodeOutgoingEdges(new ArrayList<>()));
      table.putItem(node);
    }
  }

  @Override
  public void deleteNode(InfraEntityId infraEntityId) {
    table.deleteItem(r -> r.key(k -> k.partitionValue(keyConverter.transformFrom(infraEntityId))));
  }

  @Override
  public void addOutgoingEdge(InfraEntityId entityId, InfraNodeOutgoingEdge outgoingEdge)
      throws EntityDoesNotExistException {
    var existing = getNode(entityId);
    if (existing.isEmpty())
      throw new EntityDoesNotExistException("Cannot add edge FROM non-existent node: " + entityId);

    var outgoing = getNode(outgoingEdge.getTargetNodeId());
    if (outgoing.isEmpty())
      throw new EntityDoesNotExistException(
          "Cannot create edge TO non-existent node: " + outgoingEdge.getTargetNodeId());

    var toSave = existing.get();
    var edges = toSave.getOutgoingEdges();
    if (edges == null || edges.getEdges() == null) {
      edges = new InfraNodeOutgoingEdges(new ArrayList<>());
      toSave.setOutgoingEdges(edges);
    }

    // append edge; if duplicates are not desired, filter first
    edges.getEdges().add(outgoingEdge);
    table.putItem(toSave);
  }

  @Override
  public void removeEdge(InfraEntityId entityId, InfraEntityId targetNodeId) {
    var existing = getNode(entityId);
    if (existing.isEmpty()) return;
    var node = existing.get();
    var edges = node.getOutgoingEdges();
    if (edges == null || edges.getEdges() == null || edges.getEdges().isEmpty()) {
      return;
    }
    var list = new ArrayList<>(edges.getEdges());
    list.removeIf(e -> e.getTargetNodeId().equals(targetNodeId));
    node.setOutgoingEdges(new InfraNodeOutgoingEdges(list));
    table.putItem(node);
  }

  @Override
  public List<InfraNodeOutgoingEdge> getEdgesByType(InfraEntityId entityId, DEP_TYPE depType) {
    var existing = getNode(entityId);
    if (existing.isEmpty()) return Collections.emptyList();
    var edges = existing.get().getOutgoingEdges();
    if (edges == null || edges.getEdges() == null) return Collections.emptyList();
    return edges.getEdges().stream().filter(e -> e.getDepType() == depType).toList();
  }

  @Override
  public List<InfraNodeOutgoingEdge> getAllOutgoingEdges(InfraEntityId entityId) {
    var existing = getNode(entityId);
    if (existing.isEmpty()) return Collections.emptyList();
    var edges = existing.get().getOutgoingEdges();
    if (edges == null || edges.getEdges() == null) return Collections.emptyList();
    return new ArrayList<>(edges.getEdges());
  }

  @Override
  public Optional<InfraEntityNodeDdb> getNode(InfraEntityId id) {
    var item = table.getItem(Key.builder().partitionValue(keyConverter.transformFrom(id)).build());
    return Optional.ofNullable(item);
  }
}
