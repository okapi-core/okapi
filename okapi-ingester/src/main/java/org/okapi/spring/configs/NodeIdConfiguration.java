package org.okapi.spring.configs;

import java.io.IOException;
import org.okapi.nodes.*;
import org.okapi.spring.configs.properties.NodesCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class NodeIdConfiguration {

  @Profile("prod")
  public IpSupplier ipSupplierProd() {
    return new Ec2IpSupplier();
  }

  @Profile("test")
  public IpSupplier ipSupplier() {
    String ip = System.getenv("HOSTNAME");
    ip = (ip == null) ? "localhost" : ip;
    return new FixedIpSupplier(ip);
  }

  @Bean
  public NodeIdSupplier nodeIdSupplier(@Autowired NodesCfg nodesCfg) throws IOException {
    return new FileBackedIdSupplier(nodesCfg.getNodeIdFile());
  }
}
