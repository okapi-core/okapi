# okapi-swim Integration Guide

This guide shows how to plug okapi-swim into another Spring Boot service (e.g., okapi-logs, okapi-traces) to make the app member-aware and eventually consistent via SWIM-style membership.

## Overview
- okapi-swim provides:
  - HTTP endpoints for membership events (`/okapi/swim/...`).
  - Active probing (direct + K-indirect) and dissemination.
  - Membership state transitions (ALIVE → SUSPECT → DEAD) with dedupe and TTLs.
- Add the module as a dependency, expose its beans via component scanning, and provide minimal integration pieces described below.

## What You Must Provide
1) `WhoAmI` bean (required)
- Supplies the node’s identity (id, IP, port) to okapi-swim.

2) `SeedMembersProvider` bean (optional but recommended)
- Supplies a list of initial peers. Used on startup to populate the `MemberList`.

3) Component scanning / import
- **Ensure** `org.okapi.swim` package is picked up by Spring so controllers/services/config are registered.

4) Configuration
- Provide a few `okapi.swim.*` properties (defaults exist for most).

## Maven Dependency
```xml
<dependency>
  <groupId>org.okapi</groupId>
  <artifactId>okapi-swim</artifactId>
  <version>${okapi.version}</version>
</dependency>
```

## Minimal Code You Need

- WhoAmI: identify this node
```java
import org.okapi.swim.identity.WhoAmI;
import org.springframework.stereotype.Component;

@Component
public class AppIdentity implements WhoAmI {
  @Override public String getNodeId() { return System.getenv().getOrDefault("NODE_ID", "logs-1"); }
  @Override public String getNodeIp() { return System.getenv().getOrDefault("NODE_IP", "127.0.0.1"); }
  @Override public int getNodePort() { return Integer.parseInt(System.getProperty("server.port", "8080")); }
}
```

- SeedMembersProvider: initial peers to join (optional)
```java
import java.util.List;
import org.okapi.swim.bootstrap.SeedMembersProvider;
import org.okapi.swim.ping.Member;
import org.springframework.stereotype.Component;

@Component
public class MySeedMembers implements SeedMembersProvider {
  @Override
  public List<Member> getSeedMembers() {
    return List.of(
        new Member("logs-1", "host-a", 8081),
        new Member("logs-2", "host-b", 8082)
    );
  }
}
```

- Ensure swim components are scanned
```java
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.myco.logs", "org.okapi.swim"})
public class LogsApplication { }
```
(Alternatively, place your application class in a parent package so `org.okapi.swim` is within the scan tree.)

## Configuration (application.yml)
```yaml
okapi:
  swim:
    # Probing and gossip
    k: 3                    # K peers to sample for indirect ping
    m-quorum: 2             # quorum among sampled peers
    timeout-millis: 2000    # HTTP timeouts (ms)
    broadcast-timeout-millis: 5000
    gossip-hop-count: 3

    # Liveness and scheduling
    suspect-timeout-millis: 30000
    pingSchedulerDelayMillis: 2000       # scheduler for active probing
    suspectSchedulerDelayMillis: 5000    # scheduler for SUSPECT expiry

    # Resources
    thread-pool-size: 4

    # Dedupe
    dedupe-ttl-millis: 60000
    dedupe-max-entries: 10000
```

## What Happens At Runtime
- Startup:
  - If a `SeedMembersProvider` bean is present, seeds are added to the `MemberList` automatically.
- Active probing:
  - okapi-swim periodically pings a sampled peer. On direct failure, it tries K-indirect ping.
  - If both fail, an “unhealthy” dissemination is broadcast to peers.
- Membership events:
  - The controller accepts REGISTER/ALIVE/SUSPECT and gossips them with hop count decrement.
  - SUSPECT entries expire to DEAD on a scheduler.

## Endpoints Exposed
- `POST /okapi/swim/ping` and `POST /okapi/swim/ping-indirect`
- `PUT /okapi/swim/members/{nodeId}` (REGISTER)
- `PUT /okapi/swim/members/{nodeId}/alive` (ALIVE)
- `PUT /okapi/swim/members/{nodeId}/suspect` (SUSPECT)
- `DELETE /okapi/swim/{nodeId}` (UNHEALTHY)
- `GET /okapi/swim/meta` (whoami)

## Notes & Best Practices
- Security: the endpoints are open by default; add auth (e.g., Spring filter/interceptor) if needed.
- Bootstrap: even with seeds, allow time for convergence; consider retrying joins at startup.
- Observability: log membership transitions and broadcast outcomes for operations insight.

With the `WhoAmI` bean, optional `SeedMembersProvider`, and basic properties set, okapi-swim operates as a pluggable membership module to make services (like okapi-logs and okapi-traces) member-aware and eventually consistent.