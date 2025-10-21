package org.okapi.swim.ping;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Member {
  String nodeId;
  String ip;
  int port;

}
