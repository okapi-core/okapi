package org.okapi.swim.controller;

import lombok.RequiredArgsConstructor;
import org.okapi.swim.config.SwimConfig;
import org.okapi.swim.disseminate.Disseminator;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.membership.EventDeduper;
import org.okapi.swim.membership.MembershipService;
import org.okapi.swim.ping.MemberList;
import org.okapi.swim.ping.PingService;
import org.okapi.swim.rest.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/okapi/swim")
@RequiredArgsConstructor
public class SwimController {

  private final PingService pingIndirectService;
  private final MemberList memberList;
  private final WhoAmI whoAmI;
  private final MembershipService membershipService;
  private final Disseminator disseminator;
  private final EventDeduper deduper;
  private final SwimConfig swimConfig;

  @PostMapping("/ping")
  public AckMessage ping(@RequestBody PingMessage pingMessage) {
    String nodeId = pingMessage.getFrom();
    if (nodeId != null) {
      if (memberList.getMember(nodeId) == null && pingMessage.getOwnIp() != null && pingMessage.getOwnPort() != 0) {
        var reg = new RegisterMessage(nodeId, pingMessage.getOwnIp(), pingMessage.getOwnPort(), 0L, swimConfig.getGossipHopCount());
        membershipService.applyRegister(new org.okapi.swim.ping.Member(nodeId, reg.getIp(), reg.getPort()), reg.getIncarnation());
        disseminator.disseminateRegister(decrementHop(reg));
      }
      membershipService.applyAlive(nodeId, 0L);
      var alive = new AliveMessage(nodeId, 0L, swimConfig.getGossipHopCount());
      disseminator.disseminateAlive(decrementHop(alive));
    }
    return new AckMessage();
  }

  @PostMapping("/ping-indirect")
  public AckMessage pingIndirect(@RequestBody PingRequest request) throws Exception {
    var result = pingIndirectService.ping(request.getNodeId());
    if (result.getException() != null) {
      throw result.getException();
    } else {
      return result.getResult();
    }
  }

  @DeleteMapping("/{nodeId}")
  public AckMessage delete(@PathVariable("nodeId") String nodeId) throws Exception {
    memberList.remove(nodeId);
    return new AckMessage();
  }

  @GetMapping("/meta")
  public MetaResponse metaResponse() {
    return new MetaResponse(whoAmI.getNodeId());
  }

  @PutMapping("/members/{nodeId}")
  public org.springframework.http.ResponseEntity<AckMessage> register(
      @PathVariable("nodeId") String nodeId, @RequestBody RegisterMessage message) {
    var key = new EventDeduper.Key("REGISTER", nodeId, message.getIncarnation());
    boolean first = deduper.seenOnce(key);
    boolean applied = membershipService.applyRegister(new org.okapi.swim.ping.Member(nodeId, message.getIp(), message.getPort()), message.getIncarnation());
    if (!first || !applied) {
      return org.springframework.http.ResponseEntity.noContent().build();
    }
    var msg = decrementHop(message);
    var result = disseminator.disseminateRegister(msg);
    if (result.quorumMet) {
      return org.springframework.http.ResponseEntity.ok(new AckMessage());
    } else {
      return org.springframework.http.ResponseEntity.accepted().body(new AckMessage());
    }
  }

  @PutMapping("/members/{nodeId}/alive")
  public org.springframework.http.ResponseEntity<AckMessage> alive(
      @PathVariable("nodeId") String nodeId, @RequestBody AliveMessage message) {
    var key = new EventDeduper.Key("ALIVE", nodeId, message.getIncarnation());
    boolean first = deduper.seenOnce(key);
    boolean applied = membershipService.applyAlive(nodeId, message.getIncarnation());
    if (!first || !applied) {
      return org.springframework.http.ResponseEntity.noContent().build();
    }
    var msg = decrementHop(message);
    var result = disseminator.disseminateAlive(msg);
    if (result.quorumMet) {
      return org.springframework.http.ResponseEntity.ok(new AckMessage());
    } else {
      return org.springframework.http.ResponseEntity.accepted().body(new AckMessage());
    }
  }

  @PutMapping("/members/{nodeId}/suspect")
  public org.springframework.http.ResponseEntity<AckMessage> suspect(
      @PathVariable("nodeId") String nodeId, @RequestBody SuspectMessage message) {
    var key = new EventDeduper.Key("SUSPECT", nodeId, message.getIncarnation());
    boolean first = deduper.seenOnce(key);
    boolean applied =
        membershipService.applySuspect(nodeId, message.getIncarnation(), swimConfig.getSuspectTimeoutMillis());
    if (!first || !applied) {
      return org.springframework.http.ResponseEntity.noContent().build();
    }
    var msg = decrementHop(message);
    var result = disseminator.disseminateSuspect(msg);
    if (result.quorumMet) {
      return org.springframework.http.ResponseEntity.ok(new AckMessage());
    } else {
      return org.springframework.http.ResponseEntity.accepted().body(new AckMessage());
    }
  }

  private RegisterMessage decrementHop(RegisterMessage m) {
    m.setHopCount(Math.max(0, m.getHopCount() - 1));
    return m;
  }

  private AliveMessage decrementHop(AliveMessage m) {
    m.setHopCount(Math.max(0, m.getHopCount() - 1));
    return m;
  }

  private SuspectMessage decrementHop(SuspectMessage m) {
    m.setHopCount(Math.max(0, m.getHopCount() - 1));
    return m;
  }
}
