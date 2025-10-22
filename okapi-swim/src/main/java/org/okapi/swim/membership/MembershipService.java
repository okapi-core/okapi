package org.okapi.swim.membership;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.okapi.swim.ping.Member;
import org.okapi.swim.ping.MemberList;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MembershipService {

  private final MemberList memberList;

  static class State {
    volatile LivenessStatus status;
    volatile long incarnation;
    volatile Instant lastUpdated;
    volatile Instant suspicionExpiresAt; // nullable

    State(LivenessStatus status, long incarnation) {
      this.status = status;
      this.incarnation = incarnation;
      this.lastUpdated = Instant.now();
    }
  }

  private final ConcurrentMap<String, State> states = new ConcurrentHashMap<>();

  public Optional<State> getState(String nodeId) {
    return Optional.ofNullable(states.get(nodeId));
  }

  public synchronized boolean applyRegister(Member member, long incarnation) {
    // Upsert member address
    if (memberList.getMember(member.getNodeId()) == null) {
      memberList.addMember(member);
    }
    return upsertAlive(member.getNodeId(), incarnation);
  }

  public synchronized boolean applyAlive(String nodeId, long incarnation) {
    return upsertAlive(nodeId, incarnation);
  }

  private boolean upsertAlive(String nodeId, long incarnation) {
    State current = states.get(nodeId);
    if (current == null) {
      var s = new State(LivenessStatus.ALIVE, incarnation);
      s.suspicionExpiresAt = null;
      states.put(nodeId, s);
      return true;
    }
    if (incarnation > current.incarnation
        || (incarnation == current.incarnation && current.status != LivenessStatus.ALIVE)) {
      current.status = LivenessStatus.ALIVE;
      current.incarnation = incarnation;
      current.lastUpdated = Instant.now();
      current.suspicionExpiresAt = null;
      return true;
    }
    return false; // no-op
  }

  public synchronized boolean applySuspect(
      String nodeId, long incarnation, long suspectTimeoutMillis) {
    State current = states.get(nodeId);
    if (current == null) {
      current = new State(LivenessStatus.SUSPECT, incarnation);
      states.put(nodeId, current);
      current.suspicionExpiresAt = Instant.now().plusMillis(suspectTimeoutMillis);
      return true;
    }
    if (incarnation > current.incarnation
        || (incarnation == current.incarnation && current.status == LivenessStatus.ALIVE)) {
      current.status = LivenessStatus.SUSPECT;
      current.incarnation = incarnation;
      current.lastUpdated = Instant.now();
      current.suspicionExpiresAt = Instant.now().plusMillis(suspectTimeoutMillis);
      return true;
    }
    return false;
  }

  public synchronized void tickExpireSuspicions(long suspectTimeoutMillis) {
    var now = Instant.now();
    for (var e : states.entrySet()) {
      State s = e.getValue();
      if (s.status == LivenessStatus.SUSPECT
          && s.suspicionExpiresAt != null
          && !now.isBefore(s.suspicionExpiresAt)) {
        s.status = LivenessStatus.DEAD;
        s.lastUpdated = now;
        s.suspicionExpiresAt = null;
      }
    }
  }
}
