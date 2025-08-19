package org.okapi.data.ddb;

import com.google.common.base.Preconditions;
import java.util.*;
import org.okapi.data.dao.RelationGraphDao;
import org.okapi.data.ddb.iterators.FlatteningIterator;
import org.okapi.data.ddb.iterators.MappingIterator;
import org.okapi.data.dto.RelationGraphNode;
import org.okapi.data.dto.RelationGraphNodeDdb;
import org.okapi.data.dto.TablesAndIndexes;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

public class RelationGraphDaoImpl extends AbstractDdbDao<RelationGraphNodeDdb, RelationGraphNode>
    implements RelationGraphDao {

  public RelationGraphDaoImpl(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
    super(TablesAndIndexes.RELATIONSHIP_GRAPH_TABLE, dynamoDbEnhancedClient, RelationGraphNodeDdb.class);
  }

  @Override
  public Iterator<RelationGraphNode> list(String id) {
    var results =
        table.query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(id).build()))
                .build());
    return new MappingIterator<>(new FlatteningIterator<>(results.iterator()), this::toDto);
  }

  @Override
  public Optional<RelationGraphNode> get(String left, String right) {
    var relation = table.getItem(Key.builder().partitionValue(left).sortValue(right).build());
    return Optional.ofNullable(toDto(relation));
  }

  @Override
  public void removeAll(String left, String right) {
    table.deleteItem(Key.builder().partitionValue(left).sortValue(right).build());
  }

  @Override
  public void remove(String left, String right, RelationGraphNode.RELATION_TYPE relationType) {
    var relation = get(left, right);
    if (relation.isEmpty()) return;
    var dto = relation.get();
    dto.getRelationType().remove(relationType);
    var obj = fromDto(dto);
    table.putItem(obj);
  }

  private void save(RelationGraphNodeDdb node) {
    Preconditions.checkNotNull(node);
    table.putItem(node);
  }

  private void save(RelationGraphNode node) {
    Preconditions.checkNotNull(node);
    var obj = fromDto(node);
    table.putItem(obj);
  }

  @Override
  public RelationGraphNode add(
      String left, String right, RelationGraphNode.RELATION_TYPE relationType) {
    var optionalRelationGraphNode = get(left, right);
    if (optionalRelationGraphNode.isEmpty()) {
      var relation =
          RelationGraphNodeDdb.builder()
              .entityId(left)
              .relatedEntity(right)
              .relationType(Arrays.asList(relationType))
              .build();

      save(relation);
      return toDto(relation);
    } else {
      var relation = optionalRelationGraphNode.get();
      if (!relation.getRelationType().contains(relationType)) {
        relation.getRelationType().add(relationType);
      }
      return relation;
    }
  }

  @Override
  public RelationGraphNode addAll(
      String left, String right, List<RelationGraphNode.RELATION_TYPE> relations) {
    var optionalRelationGraphNode = get(left, right);
    if (optionalRelationGraphNode.isEmpty()) {
      var relationBuilder =
          RelationGraphNodeDdb.builder()
              .entityId(left)
              .relationType(relations)
              .relatedEntity(right)
              .build();
      save(relationBuilder);
      return toDto(relationBuilder);
    } else {
      var relationNode = optionalRelationGraphNode.get();
      for (var relation : relations) {
        if (!relationNode.getRelationType().contains(relation)) {
          relationNode.getRelationType().add(relation);
        }
      }
      save(relationNode);
      return relationNode;
    }
  }

  @Override
  public List<String> pathExists(String start, String dest, EdgeSeq acceptedPath) {
    // list all outgoing edges, accept those that are to an accepted edge via an accepted node
    var depth = 0;
    // loop prevention: loop prevention is guaranteed since jumps are constrained
    Queue<String> path = new ArrayDeque<>();
    path.add(start);
    while (depth < acceptedPath.accepted().size() && !path.isEmpty()) {
      var cur = path.poll();
      var neighbors = list(cur);
      var entityType = acceptedPath.accepted().get(depth).entityType();
      var relationType = acceptedPath.accepted().get(depth).relationType();
      while (neighbors.hasNext()) {
        var neighbor = neighbors.next();
        var parsed = RelationGraphNodeDdb.parse(neighbor.getRelatedEntity());
        if (parsed.isEmpty()) continue;
        if (parsed.get().type() != entityType
            || !neighbor.getRelationType().contains(relationType)) {
          continue;
        }
        path.add(neighbor.getRelatedEntity());
        if (neighbor.getRelatedEntity().equals(dest)) return new ArrayList<>(path);
      }
      depth++;
    }
    return Collections.emptyList();
  }

  @Override
  public boolean aPathExists(String start, String dest, List<EdgeSeq> acceptedPaths) {
    for (var acceptedPath : acceptedPaths) {
      var path = pathExists(start, dest, acceptedPath);
      if (!path.isEmpty()) return true;
    }
    return false;
  }

  @Override
  public RelationGraphNodeDdb fromDto(RelationGraphNode dto) {
    if (dto == null) return null;
    return RelationGraphNodeDdb.builder()
        .entityId(dto.getEntityId())
        .relatedEntity(dto.getRelatedEntity())
        .relationType(dto.getRelationType())
        .build();
  }

  @Override
  public RelationGraphNode toDto(RelationGraphNodeDdb obj) {
    if (obj == null) return null;
    return RelationGraphNode.builder()
        .entityId(obj.getEntityId())
        .relatedEntity(obj.getRelatedEntity())
        .relationType(obj.getRelationType())
        .build();
  }
}
