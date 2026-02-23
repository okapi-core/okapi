package org.okapi.data.dao;

import java.util.List;
import java.util.Optional;
import org.okapi.data.ddb.attributes.ENTITY_TYPE;
import org.okapi.data.ddb.attributes.EdgeSeq;
import org.okapi.data.ddb.attributes.EntityId;
import org.okapi.data.ddb.attributes.OutgoingEdge;
import org.okapi.data.ddb.attributes.RELATION_TYPE;
import org.okapi.data.ddb.dao.RelationGraphNode;

public interface RelationGraphDao {

  static OutgoingEdge makeRelation(ENTITY_TYPE entityType, RELATION_TYPE relationType) {
    return new OutgoingEdge(entityType, relationType);
  }

  Optional<RelationGraphNode> getRelationsBetween(EntityId start, EntityId end);

  boolean hasRelationBetween(EntityId left, EntityId right, RELATION_TYPE relationType);

  void removeAllRelations(EntityId left, EntityId right);

  void removeRelation(EntityId left, EntityId right, RELATION_TYPE relationType);

  RelationGraphNode addRelationship(EntityId left, EntityId right, RELATION_TYPE relationType);

  RelationGraphNode addAllRelationships(
      EntityId left, EntityId right, List<RELATION_TYPE> relationType);

  boolean isPathBetween(EntityId start, EntityId end, EdgeSeq acceptedPath);

  boolean isAnyPathBetween(EntityId start, EntityId end, List<EdgeSeq> acceptedPaths);

  List<RelationGraphNode> getAllRelationsOfType(
      EntityId entityId, ENTITY_TYPE entityType, RELATION_TYPE relationType);

  List<RelationGraphNode> getAllRelationsOfNodeType(EntityId entityId, ENTITY_TYPE type);

  void deleteEntity(EntityId entityId);
}
