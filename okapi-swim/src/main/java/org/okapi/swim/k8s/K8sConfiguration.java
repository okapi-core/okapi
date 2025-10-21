package org.okapi.swim.k8s;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@Configuration
@Profile("k8s")
@ConditionalOnClass(KubernetesClient.class)
@EnableConfigurationProperties(K8sDiscoveryProperties.class)
@RequiredArgsConstructor
public class K8sConfiguration {

  @Bean(destroyMethod = "close")
  public KubernetesClient kubernetesClient() {
    return new KubernetesClientBuilder().build();
  }
}
