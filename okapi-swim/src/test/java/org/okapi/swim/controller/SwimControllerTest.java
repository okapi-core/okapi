package org.okapi.swim.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.membership.MembershipEventPublisher;
import org.okapi.swim.ping.MemberList;
import org.okapi.swim.ping.PingService;
import org.okapi.swim.rest.AckMessage;
import org.okapi.swim.rest.PingMessage;
import org.okapi.swim.rest.PingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SwimControllerTest {

  @Autowired private TestRestTemplate restTemplate;

  @MockitoBean private PingService pingIndirectService;

  @MockitoBean private MemberList memberList;

  // Satisfy Disseminator dependency during component scanning
  @MockitoBean private WhoAmI whoAmI;
  @MockitoBean MembershipEventPublisher membershipEventPublisher;

  @Test
  void ping_shouldReturnOk() {
    ResponseEntity<AckMessage> response =
        restTemplate.postForEntity(
            "/okapi/swim/ping",
            new PingMessage(whoAmI.getNodeId(), whoAmI.getNodeIp(), whoAmI.getNodePort()),
            AckMessage.class);
    assertEquals(200, response.getStatusCode().value());
  }

  @Test
  void delete_shouldRemoveMember() {
    ResponseEntity<AckMessage> response =
        restTemplate.exchange("/okapi/swim/node-z", HttpMethod.DELETE, null, AckMessage.class);
    assertEquals(200, response.getStatusCode().value());
    verify(memberList).remove("node-z");
  }

  @Test
  void pingIndirect_propagatesAck() {
    when(pingIndirectService.ping("n1"))
        .thenReturn(new org.okapi.swim.Result<>(new AckMessage(), null));

    ResponseEntity<AckMessage> response =
        restTemplate.postForEntity(
            "/okapi/swim/ping-indirect", new PingRequest("n1"), AckMessage.class);
    assertEquals(200, response.getStatusCode().value());
  }
}
