package org.okapi.swim.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RegisterMessage {
  private String nodeId;
  private String ip;
  private int port;
  private long incarnation;
  private int hopCount;
}

