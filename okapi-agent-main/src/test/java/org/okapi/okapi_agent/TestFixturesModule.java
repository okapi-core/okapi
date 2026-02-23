package org.okapi.okapi_agent;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.Random;
import java.util.random.RandomGenerator;
import mockwebserver3.MockWebServer;
import org.okapi.okapi_agent.config.GatewayConfig;

public class TestFixturesModule extends AbstractModule {
  public record MockServerPort(int port) {}

  @Provides
  @Singleton
  public MockServerPort mockServerPort() {
    var randomPort = Random.from(RandomGenerator.getDefault()).nextInt(10000, 60000);
    return new MockServerPort(randomPort);
  }

  @Provides
  @Singleton
  public GatewayConfig gatewayConfig(MockServerPort mockServerPort) {
    return new GatewayConfig("http://localhost:" + mockServerPort.port(), "test");
  }

  @Provides
  @Singleton
  public MockWebServer provideMockWebServer() {
    return new MockWebServer();
  }
}
