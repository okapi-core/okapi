/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.testmodules.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.okapi.identity.InMemoryMemberLists;
import org.okapi.identity.Member;
import org.okapi.identity.MemberList;
import org.okapi.identity.WhoAmI;
import org.okapi.sharding.FixedMapShardAssigner;
import org.okapi.sharding.ShardAssigner;
import org.okapi.sharding.ShardRegistry;
import org.okapi.zk.FakeZkClient;
import org.okapi.zk.NamespacedZkClient;

public class TestShardingModule extends AbstractModule {
  private final WhoAmI whoAmI;
  private final List<Integer> assignedShards;
  private final Map<Long, Integer> blockShardMap;
  private final String remoteNodeId;

  public TestShardingModule(
      WhoAmI whoAmI,
      List<Integer> assignedShards,
      Map<Long, Integer> blockShardMap,
      String remoteNodeId) {
    this.whoAmI = whoAmI;
    this.assignedShards = assignedShards;
    this.blockShardMap = blockShardMap;
    this.remoteNodeId = remoteNodeId;
  }

  @Override
  protected void configure() {
    bind(WhoAmI.class).toInstance(whoAmI);
    var fixedMapShardAssigner = new FixedMapShardAssigner<String>(blockShardMap, Map.of());
    bind(new TypeLiteral<FixedMapShardAssigner<String>>() {}).toInstance(fixedMapShardAssigner);
    bind(new TypeLiteral<ShardAssigner<String>>() {}).toInstance(fixedMapShardAssigner);
  }

  @Provides
  @Singleton
  NamespacedZkClient provideFakeZkClient() {
    return new FakeZkClient(assignedShards, new HashMap<>());
  }

  @Provides
  @Singleton
  ShardRegistry provideShardRegistry(NamespacedZkClient namespacedZkClient) {
    return new ShardRegistry(namespacedZkClient);
  }

  @Provides
  @Singleton
  MemberList provideMemberList() {
    var members = new InMemoryMemberLists(whoAmI);
    members.addMember(new Member(whoAmI.getNodeId(), whoAmI.getNodeIp(), whoAmI.getNodePort()));
    members.addMember(new Member(remoteNodeId, "127.0.0.1", 9090));
    return members;
  }
}
