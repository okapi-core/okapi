package org.okapi.data.dao;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.management.relation.RelationType;
import org.okapi.data.dto.RelationGraphNode;

public interface RelationGraphDao {

  record RelationType(
      RelationGraphNode.ENTITY_TYPE entityType, RelationGraphNode.RELATION_TYPE relationType) {}

  record EdgeSeq(List<RelationType> accepted) {}

  static RelationType makeRelation(
      RelationGraphNode.ENTITY_TYPE entityType, RelationGraphNode.RELATION_TYPE relationType) {
    return new RelationType(entityType, relationType);
  }

  Iterator<RelationGraphNode> list(String id);

  Optional<RelationGraphNode> get(String left, String right);

  void removeAll(String left, String right);

  void remove(String left, String right, RelationGraphNode.RELATION_TYPE relationType);

  RelationGraphNode add(String left, String right, RelationGraphNode.RELATION_TYPE relationType);

  RelationGraphNode addAll(
      String left, String right, List<RelationGraphNode.RELATION_TYPE> relationType);

  List<String> pathExists(String start, String dest, EdgeSeq acceptedPath);

  boolean aPathExists(String start, String dest, List<EdgeSeq> acceptedPaths);
}
