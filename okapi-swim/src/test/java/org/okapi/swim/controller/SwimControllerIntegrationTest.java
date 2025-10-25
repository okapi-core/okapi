package org.okapi.swim.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.okapi.swim.bootstrap.SeedMembersProvider;
import org.okapi.swim.disseminate.Disseminator;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.membership.EventDeduper;
import org.okapi.swim.membership.MembershipEventPublisher;
import org.okapi.swim.membership.MembershipService;
import org.okapi.swim.ping.Member;
import org.okapi.swim.ping.MemberList;
import org.okapi.swim.rest.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = {
      // keep deterministic hop count for ping->dissemination
      "okapi.swim.gossip-hop-count=3",
      // keep timeouts short in test env
      "okapi.swim.timeout-millis=200",
      // executor size small for tests
      "okapi.swim.thread-pool-size=2"
    })
class SwimControllerIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @MockitoBean private MembershipService membershipService;

  @MockitoBean private Disseminator disseminator;

  @MockitoBean private EventDeduper deduper;

  @MockitoBean private MemberList memberList;

  @MockitoBean private WhoAmI whoAmI;
  @MockitoBean MembershipEventPublisher membershipEventPublisher;
  @MockitoBean private SeedMembersProvider seedMembersProvider;

  // --- Meta ---
  @Test
  void meta_shouldReturnNodeIdFromWhoAmI() {
    when(whoAmI.getNodeId()).thenReturn("self-node");
    ResponseEntity<MetaResponse> resp =
        restTemplate.getForEntity("/fleet/meta", MetaResponse.class);
    assertEquals(200, resp.getStatusCode().value());
    assertNotNull(resp.getBody());
    assertEquals("self-node", resp.getBody().getIAm());
  }

  // --- Register ---
  @Test
  void register_firstEventApplied_quorumMet_returns200_andDecrementsHop() {
    when(deduper.seenOnce(any())).thenReturn(true);
    when(membershipService.applyRegister(any(Member.class), anyLong())).thenReturn(true);
    when(disseminator.disseminateRegister(any(RegisterMessage.class)))
        .thenReturn(new Disseminator.BroadcastResult(1, 0, true));

    RegisterMessage msg = new RegisterMessage("n1", "1.2.3.4", 8080, 5L, 3);
    ResponseEntity<AckMessage> resp =
        restTemplate.exchange(
            "/fleet/members/n1",
            HttpMethod.PUT,
            new HttpEntity<>(msg, new HttpHeaders()),
            AckMessage.class);

    assertEquals(200, resp.getStatusCode().value());
    assertNotNull(resp.getBody());

    // verify upsert
    ArgumentCaptor<Member> mCap = ArgumentCaptor.forClass(Member.class);
    verify(membershipService).applyRegister(mCap.capture(), eq(5L));
    assertEquals("n1", mCap.getValue().getNodeId());
    assertEquals("1.2.3.4", mCap.getValue().getIp());
    assertEquals(8080, mCap.getValue().getPort());

    // verify hop decrement
    ArgumentCaptor<RegisterMessage> cap = ArgumentCaptor.forClass(RegisterMessage.class);
    verify(disseminator).disseminateRegister(cap.capture());
    assertEquals(2, cap.getValue().getHopCount());
  }

  @Test
  void register_firstEventApplied_quorumNotMet_returns202() {
    when(deduper.seenOnce(any())).thenReturn(true);
    when(membershipService.applyRegister(any(Member.class), anyLong())).thenReturn(true);
    when(disseminator.disseminateRegister(any(RegisterMessage.class)))
        .thenReturn(new Disseminator.BroadcastResult(0, 1, false));

    RegisterMessage msg = new RegisterMessage("n2", "10.0.0.2", 9000, 7L, 4);
    ResponseEntity<AckMessage> resp =
        restTemplate.exchange(
            "/fleet/members/n2",
            HttpMethod.PUT,
            new HttpEntity<>(msg, new HttpHeaders()),
            AckMessage.class);

    assertEquals(202, resp.getStatusCode().value());
    assertNotNull(resp.getBody());
  }

  @Test
  void register_duplicateEvent_returns204_noDissemination() {
    when(deduper.seenOnce(any())).thenReturn(false);
    when(membershipService.applyRegister(any(Member.class), anyLong())).thenReturn(true);

    RegisterMessage msg = new RegisterMessage("n3", "10.0.0.3", 9100, 3L, 5);
    ResponseEntity<Void> resp =
        restTemplate.exchange(
            "/fleet/members/n3",
            HttpMethod.PUT,
            new HttpEntity<>(msg, new HttpHeaders()),
            Void.class);

    assertEquals(204, resp.getStatusCode().value());
    verify(disseminator, never()).disseminateRegister(any());
  }

  // --- Alive ---
  @Test
  void alive_firstEventApplied_quorumMet_returns200_andDecrementsHop() {
    when(deduper.seenOnce(any())).thenReturn(true);
    when(membershipService.applyAlive(anyString(), anyLong())).thenReturn(true);
    when(disseminator.disseminateAlive(any(AliveMessage.class)))
        .thenReturn(new Disseminator.BroadcastResult(1, 0, true));

    AliveMessage msg = new AliveMessage("n4", 11L, 4);
    ResponseEntity<AckMessage> resp =
        restTemplate.exchange(
            "/fleet/members/n4/alive",
            HttpMethod.PUT,
            new HttpEntity<>(msg, new HttpHeaders()),
            AckMessage.class);

    assertEquals(200, resp.getStatusCode().value());
    assertNotNull(resp.getBody());

    ArgumentCaptor<AliveMessage> cap = ArgumentCaptor.forClass(AliveMessage.class);
    verify(disseminator).disseminateAlive(cap.capture());
    assertEquals(3, cap.getValue().getHopCount());
  }

  @Test
  void alive_noop_returns204_noDissemination() {
    when(deduper.seenOnce(any())).thenReturn(true);
    when(membershipService.applyAlive(anyString(), anyLong())).thenReturn(false);

    AliveMessage msg = new AliveMessage("n5", 2L, 2);
    ResponseEntity<Void> resp =
        restTemplate.exchange(
            "/fleet/members/n5/alive",
            HttpMethod.PUT,
            new HttpEntity<>(msg, new HttpHeaders()),
            Void.class);

    assertEquals(204, resp.getStatusCode().value());
    verify(disseminator, never()).disseminateAlive(any());
  }

  // --- Suspect ---
  @Test
  void suspect_firstEventApplied_quorumNotMet_returns202_andDecrementsHop() {
    when(deduper.seenOnce(any())).thenReturn(true);
    when(membershipService.applySuspect(anyString(), anyLong(), anyLong())).thenReturn(true);
    when(disseminator.disseminateSuspect(any(SuspectMessage.class)))
        .thenReturn(new Disseminator.BroadcastResult(0, 1, false));

    SuspectMessage msg = new SuspectMessage("n6", 13L, 2);
    ResponseEntity<AckMessage> resp =
        restTemplate.exchange(
            "/fleet/members/n6/suspect",
            HttpMethod.PUT,
            new HttpEntity<>(msg, new HttpHeaders()),
            AckMessage.class);

    assertEquals(202, resp.getStatusCode().value());
    assertNotNull(resp.getBody());

    ArgumentCaptor<SuspectMessage> cap = ArgumentCaptor.forClass(SuspectMessage.class);
    verify(disseminator).disseminateSuspect(cap.capture());
    assertEquals(1, cap.getValue().getHopCount());
  }

  @Test
  void suspect_duplicate_returns204_noDissemination() {
    when(deduper.seenOnce(any())).thenReturn(false);
    when(membershipService.applySuspect(anyString(), anyLong(), anyLong())).thenReturn(true);

    SuspectMessage msg = new SuspectMessage("n7", 3L, 1);
    ResponseEntity<Void> resp =
        restTemplate.exchange(
            "/fleet/members/n7/suspect",
            HttpMethod.PUT,
            new HttpEntity<>(msg, new HttpHeaders()),
            Void.class);

    assertEquals(204, resp.getStatusCode().value());
    verify(disseminator, never()).disseminateSuspect(any());
  }

  // --- Ping ---
  @Test
  void ping_withNewSender_registersAndAliveAndDisseminates_withDecrementedHop() {
    when(whoAmI.getNodeId()).thenReturn("self-node");
    when(whoAmI.getNodeIp()).thenReturn("127.0.0.1");
    when(whoAmI.getNodePort()).thenReturn(8080);

    when(memberList.getMember("fromA")).thenReturn(null);

    ResponseEntity<AckMessage> resp =
        restTemplate.postForEntity(
            "/fleet/ping", new PingMessage("fromA", "2.2.2.2", 9001), AckMessage.class);

    assertEquals(200, resp.getStatusCode().value());

    // membership updates
    verify(membershipService).applyRegister(any(Member.class), eq(0L));
    verify(membershipService).applyAlive(eq("fromA"), eq(0L));

    // disseminations with decremented hop (gossipHopCount=3 => disseminated with 2)
    ArgumentCaptor<AliveMessage> aliveCap = ArgumentCaptor.forClass(AliveMessage.class);
    verify(disseminator).disseminateAlive(aliveCap.capture());
    assertEquals(2, aliveCap.getValue().getHopCount());

    ArgumentCaptor<RegisterMessage> regCap = ArgumentCaptor.forClass(RegisterMessage.class);
    verify(disseminator).disseminateRegister(regCap.capture());
    assertEquals(2, regCap.getValue().getHopCount());
  }

  @Test
  void ping_missingOwnIpOrPort_doesNotRegister_butAliveDisseminated() {
    when(whoAmI.getNodeId()).thenReturn("self-node");
    when(memberList.getMember("fromB")).thenReturn(null);

    ResponseEntity<AckMessage> resp =
        restTemplate.postForEntity(
            "/fleet/ping",
            // ownIp absent, ownPort 0 -> should not register
            new PingMessage("fromB", null, 0),
            AckMessage.class);

    assertEquals(200, resp.getStatusCode().value());
    verify(membershipService, never()).applyRegister(any(Member.class), anyLong());
    verify(disseminator, never()).disseminateRegister(any());
    verify(membershipService).applyAlive(eq("fromB"), eq(0L));
    verify(disseminator).disseminateAlive(any(AliveMessage.class));
  }
}
