package org.okapi.swim.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SuspectMessage {
  private String nodeId;
  private long incarnation;
  private int hopCount;
}

