package org.okapi.logs;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StaticConfigurationTests {

  @Test
  public void testHashing() {
    var hash = StaticConfiguration.rkHash("nodeId");
    assertTrue(hash < StaticConfiguration.N_BLOCKS);
  }

  @Test
  public void testStreamHashing() {
    var hr = System.currentTimeMillis() / 3600_000L;
    var hash = StaticConfiguration.hashLogStream("tenant", "stream", hr);
    assertTrue(hash < StaticConfiguration.N_BLOCKS);
  }

  @Test
  public void testNodeHash() {
    var hash = StaticConfiguration.hashNode(UUID.randomUUID().toString());
    assertTrue(hash < StaticConfiguration.N_BLOCKS);
  }
}
