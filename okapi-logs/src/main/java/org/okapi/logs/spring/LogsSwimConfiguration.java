package org.okapi.logs.spring;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import org.okapi.swim.bootstrap.SeedMembersProvider;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.ping.Member;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class LogsSwimConfiguration {

  // Default WhoAmI for non-k8s: random UUID id, local IP, server.port
  @Bean
  @Profile("!k8s")
  @ConditionalOnMissingBean(WhoAmI.class)
  public WhoAmI defaultWhoAmI(@Value("${server.port:8080}") int serverPort) {
    return new WhoAmI() {
      private final String nodeId = UUID.randomUUID().toString();

      @Override
      public String getNodeId() {
        return nodeId;
      }

      @Override
      public String getNodeIp() {
        try {
          return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
          return "127.0.0.1";
        }
      }

      @Override
      public int getNodePort() {
        return serverPort;
      }
    };
  }

  // In test profile, seed only self so the member list remains single-node
  @Bean
  @Profile("test")
  public SeedMembersProvider testOnlySelfSeedProvider(WhoAmI whoAmI) {
    return () -> List.of(new Member(whoAmI.getNodeId(), whoAmI.getNodeIp(), whoAmI.getNodePort()));
  }
}
