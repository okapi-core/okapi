package org.okapi.swim.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PingMessage {
    private String from;
    private String ownIp;
    private int ownPort;
}
