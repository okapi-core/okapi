package org.okapi.data.ddb.dao;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.okapi.data.dao.UserEntityRelationsDao;
import org.okapi.data.ddb.attributes.EntityRelationId;
import org.okapi.data.ddb.attributes.serialization.EntityRelationIdConverter;
import org.okapi.data.dto.TablesAndIndexes;
import org.okapi.data.dto.UserEntityRelations;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

public class UserEntityRelationsDaoImpl implements UserEntityRelationsDao {

  DynamoDbTable<UserEntityRelations> table;
  EntityRelationIdConverter converter = new EntityRelationIdConverter();

  @Inject
  public UserEntityRelationsDaoImpl(DynamoDbEnhancedClient enhancedClient) {
    this.table =
        enhancedClient.table(
            TablesAndIndexes.USER_ENTITY_RELATIONS_TABLE,
            TableSchema.fromBean(UserEntityRelations.class));
  }

  @Override
  public Optional<UserEntityRelations> getRelation(String userId, EntityRelationId edgeId) {
    var convertedEdgeId = converter.transformFrom(edgeId);
    var query = table.getItem(r -> r.key(k -> k.partitionValue(userId).sortValue(convertedEdgeId)));
    return Optional.ofNullable(query);
  }

  @Override
  public List<UserEntityRelations> listUserRelations(String userId) {
    var query =
        table.query(
            r ->
                r.queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build())));
    return query.items().stream().toList();
  }

  @Override
  public void createRelation(UserEntityRelations userEntityRelations) {
    table.putItem(userEntityRelations);
  }

  @Override
  public void deleteRelation(String userId, EntityRelationId edgeId) {
    var convertedEdgeId = converter.transformFrom(edgeId);
    table.deleteItem(r -> r.key(k -> k.partitionValue(userId).sortValue(convertedEdgeId)));
  }
}
