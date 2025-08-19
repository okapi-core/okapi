package org.okapi.ip;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FixedIpSupplier implements IpSupplier{
    String host;
    int port;
    @Override
    public String getIp() {
        return host + ":" + port;
    }
}
