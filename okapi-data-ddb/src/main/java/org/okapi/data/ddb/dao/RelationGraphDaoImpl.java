package org.okapi.data.ddb.dao;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.*;
import lombok.AllArgsConstructor;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.ddb.attributes.ENTITY_TYPE;
import org.okapi.data.ddb.attributes.EdgeSeq;
import org.okapi.data.ddb.attributes.EntityId;
import org.okapi.data.ddb.attributes.RELATION_TYPE;
import org.okapi.data.ddb.iterators.FlatteningIterator;
import org.okapi.data.ddb.iterators.MappingIterator;
import org.okapi.data.dto.*;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

public class RelationGraphDaoImpl extends AbstractDdbDao<RelationGraphNodeDdb, RelationGraphNode>
    implements RelationGraphDao {

  @Inject
  public RelationGraphDaoImpl(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
    super(
        TablesAndIndexes.RELATIONSHIP_GRAPH_TABLE,
        dynamoDbEnhancedClient,
        RelationGraphNodeDdb.class);
  }

  public String getEntityIdString(EntityId id) {
    return id.type().name() + ":" + id.id();
  }

  private Iterator<RelationGraphNode> getAllRelationsOf(EntityId id) {
    var results =
        table.query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(getEntityIdString(id)).build()))
                .build());
    return new MappingIterator<>(
        new FlatteningIterator<>(results.iterator()), this::toRelationGraphNode);
  }

  @Override
  public Optional<RelationGraphNode> getRelationsBetween(EntityId left, EntityId right) {
    var relation =
        table.getItem(
            Key.builder()
                .partitionValue(getEntityIdString(left))
                .sortValue(getEntityIdString(right))
                .build());
    return Optional.ofNullable(toRelationGraphNode(relation));
  }

  @Override
  public boolean hasRelationBetween(EntityId left, EntityId right, RELATION_TYPE relationType) {
    var relationNode = getRelationsBetween(left, right);
    return relationNode
        .map(relationGraphNode -> relationGraphNode.getRelationships().contains(relationType))
        .orElse(false);
  }

  @Override
  public void removeAllRelations(EntityId left, EntityId right) {
    table.deleteItem(
        Key.builder()
            .partitionValue(getEntityIdString(left))
            .sortValue(getEntityIdString(right))
            .build());
    table.deleteItem(
        Key.builder()
            .partitionValue(getEntityIdString(right))
            .sortValue(getEntityIdString(left))
            .build());
  }

  @Override
  public void removeRelation(EntityId left, EntityId right, RELATION_TYPE relation) {
    var relationNode = getRelationsBetween(left, right);
    if (relationNode.isEmpty()) return;
    var dto = relationNode.get();
    var obj = getDdbNodeFromDto(dto);
    dto.getRelationships().remove(relation);
    table.putItem(obj);
  }

  private void save(RelationGraphNodeDdb node) {
    Preconditions.checkNotNull(node);
    table.putItem(node);
  }

  private void save(RelationGraphNode node) {
    Preconditions.checkNotNull(node);
    var obj = getDdbNodeFromDto(node);
    table.putItem(obj);
  }

  @Override
  public RelationGraphNode addRelationship(
      EntityId left, EntityId right, RELATION_TYPE relationType) {
    var optionalRelationGraphNode = getRelationsBetween(left, right);
    if (optionalRelationGraphNode.isEmpty()) {
      var relation =
          RelationGraphNodeDdb.builder()
              .relatedEntityType(right.type())
              .entityId(getEntityIdString(left))
              .relatedEntity(getEntityIdString(right))
              .relationships(Arrays.asList(relationType))
              .build();

      save(relation);
      return toRelationGraphNode(relation);
    } else {
      var relation = optionalRelationGraphNode.get();
      if (!relation.getRelationships().contains(relationType)) {
        relation.getRelationships().add(relationType);
      }
      if (relation.getRelatedEntityType() != right.type()) {
        relation.setRelatedEntityType(right.type());
      }
      save(relation);
      return relation;
    }
  }

  @Override
  public RelationGraphNode addAllRelationships(
      EntityId left, EntityId right, List<RELATION_TYPE> relations) {
    var optionalRelationGraphNode = getRelationsBetween(left, right);
    if (optionalRelationGraphNode.isEmpty()) {
      var relationBuilder =
          RelationGraphNodeDdb.builder()
              .entityId(getEntityIdString(left))
              .relationships(relations)
              .relatedEntity(getEntityIdString(right))
              .build();
      save(relationBuilder);
      return toRelationGraphNode(relationBuilder);
    } else {
      var relationNode = optionalRelationGraphNode.get();
      for (var relation : relations) {
        if (!relationNode.getRelationships().contains(relation)) {
          relationNode.getRelationships().add(relation);
        }
      }
      save(relationNode);
      return relationNode;
    }
  }

  @AllArgsConstructor
  public static class PathNode {
    public int pathIndex;
    public EntityId node;
  }

  @Override
  public boolean isPathBetween(EntityId start, EntityId dest, EdgeSeq acceptedPath) {
    // list all outgoing edges, accept those that are to an accepted edge via an accepted node
    var destId = getEntityIdString(dest);
    // loop prevention: loop prevention is guaranteed since jumps are constrained
    Queue<PathNode> nodes = new ArrayDeque<>();
    nodes.add(new PathNode(0, start));
    while (!nodes.isEmpty()) {
      var pathNode = nodes.poll();
      if (pathNode.pathIndex >= acceptedPath.accepted().size()) {
        continue;
      }
      if (pathNode.pathIndex == acceptedPath.accepted().size() - 1) {
        // last hop, check direct relationship
        var relations = getRelationsBetween(pathNode.node, dest);
        if (relations.isPresent()) {
          var relNode = relations.get();
          var requiredEdge = acceptedPath.accepted().get(pathNode.pathIndex);
          if (relNode.getRelatedEntityType() == requiredEdge.outgoingNodeType()
              && relNode.getRelationships().contains(requiredEdge.relationType())) {
            return true;
          }
        }
        continue;
      }
      var iterator = getAllRelationsOf(pathNode.node);
      while (iterator.hasNext()) {
        var relNode = iterator.next();
        var requiredEdge = acceptedPath.accepted().get(pathNode.pathIndex);
        if (relNode.getRelatedEntityType() == requiredEdge.outgoingNodeType()
            && relNode.getRelationships().contains(requiredEdge.relationType())) {
          var nextNodeOpt = RelationGraphNodeDdb.parse(relNode.getRelatedEntity());
          nextNodeOpt.ifPresent(
              entityId -> nodes.add(new PathNode(pathNode.pathIndex + 1, entityId)));
        }
      }
    }
    return false;
  }

  @Override
  public boolean isAnyPathBetween(EntityId start, EntityId dest, List<EdgeSeq> acceptedPaths) {
    for (var acceptedPath : acceptedPaths) {
      if (isPathBetween(start, dest, acceptedPath)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<RelationGraphNode> getAllRelationsOfType(
      EntityId entityId, ENTITY_TYPE entityType, RELATION_TYPE relationType) {
    var allRelations = getAllRelationsOf(entityId);
    var filteredRelations = new ArrayList<RelationGraphNode>();
    while (allRelations.hasNext()) {
      var relation = allRelations.next();
      if (relation.getRelatedEntityType() == entityType
          && relation.getRelationships().contains(relationType)) {
        filteredRelations.add(relation);
      }
    }
    return filteredRelations;
  }

  @Override
  public List<RelationGraphNode> getAllRelationsOfNodeType(EntityId entityId, ENTITY_TYPE type) {
    var allRelations = getAllRelationsOf(entityId);
    var filteredRelations = new ArrayList<RelationGraphNode>();
    while (allRelations.hasNext()) {
      var relation = allRelations.next();
      if (relation.getRelatedEntityType() == type) {
        filteredRelations.add(relation);
      }
    }
    return filteredRelations;
  }

  @Override
  public void deleteEntity(EntityId entityId) {
    var allRelations = getAllRelationsOf(entityId);
    while (allRelations.hasNext()) {
      var relation = allRelations.next();
      var relatedEntityOpt = RelationGraphNodeDdb.parse(relation.getRelatedEntity());
      relatedEntityOpt.ifPresent(
          relatedEntity ->
              table.deleteItem(
                  Key.builder()
                      .partitionValue(getEntityIdString(relatedEntity))
                      .sortValue(getEntityIdString(entityId))
                      .build()));
      table.deleteItem(
          Key.builder()
              .partitionValue(getEntityIdString(entityId))
              .sortValue(relation.getRelatedEntity())
              .build());
    }
  }

  @Override
  public RelationGraphNodeDdb getDdbNodeFromDto(RelationGraphNode dto) {
    if (dto == null) return null;
    return RelationGraphNodeDdb.builder()
        .entityId(dto.getEntityId())
        .relatedEntity(dto.getRelatedEntity())
        .relationships(dto.getRelationships())
        .relatedEntityType(dto.getRelatedEntityType())
        .build();
  }

  @Override
  public RelationGraphNode toRelationGraphNode(RelationGraphNodeDdb obj) {
    if (obj == null) return null;
    return RelationGraphNode.builder()
        .entityId(obj.getEntityId())
        .relatedEntity(obj.getRelatedEntity())
        .relationships(obj.getRelationships())
        .relatedEntityType(obj.getRelatedEntityType())
        .build();
  }
}
