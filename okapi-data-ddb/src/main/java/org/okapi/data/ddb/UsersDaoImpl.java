package org.okapi.data.ddb;

import com.google.common.collect.Lists;
import java.util.*;
import org.okapi.bcrypt.BCrypt;
import org.okapi.data.dao.UsersDao;
import org.okapi.data.ddb.iterators.FlatteningIterator;
import org.okapi.data.ddb.iterators.MappingIterator;
import org.okapi.data.dto.*;
import org.okapi.data.exceptions.UserAlreadyExistsException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

public class UsersDaoImpl implements UsersDao {

  DynamoDbEnhancedClient dynamoDbEnhancedClient;
  DynamoDbTable<UserDtoDdb> usersTable;
  DynamoDbTable<UserRoleRelationDtoDdb> roleRelationsTable;

  public UsersDaoImpl(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
    this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    usersTable =
        this.dynamoDbEnhancedClient.table(
            TablesAndIndexes.USERS_TABLE, TableSchema.fromBean(UserDtoDdb.class));
    roleRelationsTable =
        this.dynamoDbEnhancedClient.table(
            TablesAndIndexes.USER_ROLE_RELATIONS,
            TableSchema.fromBean(UserRoleRelationDtoDdb.class));
  }

  @Override
  public Optional<UserDto> get(String userId) {
    var obj = usersTable.getItem(Key.builder().partitionValue(userId).build());
    return Optional.ofNullable(toDto(obj));
  }

  @Override
  public Optional<UserDto> getWithEmail(String email) {
    var idx = usersTable.index(TablesAndIndexes.USERS_BY_EMAIL_GSI);
    var query =
        idx.query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(email).build()))
                .build());
    var lists = Lists.newArrayList(new FlatteningIterator<>(query.iterator()));
    if (lists.isEmpty()) return Optional.empty();
    else return Optional.of(toDto(lists.getFirst()));
  }

  @Override
  public UserDto create(String firstName, String lastName, String email, String password)
      throws UserAlreadyExistsException {
    var userId = UUID.randomUUID().toString();
    String hashed;
    if (password == null) {
      hashed = BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt());
    } else {
      hashed = BCrypt.hashpw(password, BCrypt.gensalt());
    }
    var existingUser = getWithEmail(email);
    if (existingUser.isPresent()) {
      throw new UserAlreadyExistsException();
    }
    var user =
        UserDtoDdb.builder()
            .userId(userId)
            .status(UserDto.UserStatus.ACTIVE)
            .email(email)
            .firstName(firstName)
            .lastName(lastName)
            .hashedPassword(hashed)
            .build();
    usersTable.putItem(user);
    return toDto(user);
  }

  @Override
  public Iterator<UserDto> list() {
    var scan = usersTable.scan();
    return new MappingIterator<>(new FlatteningIterator<>(scan.iterator()), this::toDto);
  }

  @Override
  public Iterator<UserRoleRelationDto> listRolesByUserId(String userId) {
    // query by hash key as userId
    var results =
        roleRelationsTable.query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build()))
                .build());
    return new MappingIterator<>(new FlatteningIterator<>(results.iterator()), this::toDto);
  }

  @Override
  public Optional<UserRoleRelationDto> checkRole(String userId, String role) {
    var userRoleRelation =
        roleRelationsTable.getItem(Key.builder().partitionValue(userId).sortValue(role).build());
    if (userRoleRelation == null) {
      return Optional.empty();
    } else if (userRoleRelation.getStatus() == UserRoleRelationDto.STATUS.INACTIVE) {
      return Optional.empty();
    } else {
      return Optional.of(toDto(userRoleRelation));
    }
  }

  @Override
  public Iterator<String> listUsersWithRole(String role) {
    var idx = roleRelationsTable.index(TablesAndIndexes.ROLE_TO_USER_GSI);
    var users =
        idx.query(
            QueryEnhancedRequest.builder()
                .queryConditional(
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(role).build()))
                .build());
    return Lists.newArrayList(new FlatteningIterator<>(users.iterator())).stream()
        .map(UserRoleRelationDtoDdb::getUserId)
        .map(this::get)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(UserDto::getEmail)
        .toList()
        .iterator();
  }

  @Override
  public UserRoleRelationDto grantRole(String userId, String role) {
    var roleRelation =
        UserRoleRelationDtoDdb.builder()
            .userId(userId)
            .role(role)
            .status(UserRoleRelationDto.STATUS.ACTIVE)
            .build();
    roleRelationsTable.putItem(roleRelation);
    return toDto(roleRelation);
  }

  @Override
  public void revokeRole(String userId, String role) {
    roleRelationsTable.deleteItem(Key.builder().partitionValue(userId).sortValue(role).build());
  }

  public UserDtoDdb fromDto(UserDto dto) {
    if (dto == null) return null;
    else
      return UserDtoDdb.builder()
          .status(dto.getStatus())
          .email(dto.getEmail())
          .userId(dto.getUserId())
          .firstName(dto.getFirstName())
          .lastName(dto.getLastName())
          .hashedPassword(dto.getHashedPassword())
          .build();
  }

  public UserDto toDto(UserDtoDdb obj) {
    if (obj == null) return null;
    else
      return UserDto.builder()
          .status(obj.getStatus())
          .email(obj.getEmail())
          .userId(obj.getUserId())
          .firstName(obj.getFirstName())
          .lastName(obj.getLastName())
          .hashedPassword(obj.getHashedPassword())
          .build();
  }

  public UserRoleRelationDto toDto(UserRoleRelationDtoDdb userRoleRelationDtoDdb) {
    if (userRoleRelationDtoDdb == null) {
      return null;
    }
    return UserRoleRelationDto.builder()
        .userId(userRoleRelationDtoDdb.getUserId())
        .status(userRoleRelationDtoDdb.getStatus())
        .role(userRoleRelationDtoDdb.getRole())
        .status(userRoleRelationDtoDdb.getStatus())
        .build();
  }

  public UserRoleRelationDtoDdb fromDto(UserRoleRelationDto userRoleRelationDto) {
    if (userRoleRelationDto == null) {
      return null;
    }
    return UserRoleRelationDtoDdb.builder()
        .userId(userRoleRelationDto.getUserId())
        .status(userRoleRelationDto.getStatus())
        .role(userRoleRelationDto.getRole())
        .status(userRoleRelationDto.getStatus())
        .build();
  }
}
