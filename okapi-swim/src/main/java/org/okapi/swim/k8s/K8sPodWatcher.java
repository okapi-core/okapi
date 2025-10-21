package org.okapi.swim.k8s;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.ping.Member;
import org.okapi.swim.ping.MemberList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("k8s")
@ConditionalOnClass(KubernetesClient.class)
@RequiredArgsConstructor
public class K8sPodWatcher implements Watcher<Pod> {

  private final KubernetesClient client;
  private final K8sDiscoveryProperties properties;
  private final K8sSeedRegistry registry; // reuse cache + meta resolve
  private final MemberList memberList;
  private final WhoAmI whoAmI;

  private io.fabric8.kubernetes.client.Watch watch;

  @Value("${server.port}")
  private int serverPort;

  @PostConstruct
  public void start() {
    reconnectWithBackoff(0);
  }

  @PreDestroy
  public void stop() {
    if (watch != null) watch.close();
  }

  private void reconnectWithBackoff(long delayMs) {
    if (delayMs > 0) {
      try {
        Thread.sleep(Math.min(delayMs, properties.getWatchReconnectMaxBackoffMillis()));
      } catch (InterruptedException ignored) {
      }
    }
    String ns = effectiveNamespace();
    String labelValue = properties.getOkapiServiceLabelValue();
    if (labelValue == null || labelValue.isBlank()) return;
    if (watch != null) watch.close();
    watch = client.pods().inNamespace(ns).withLabel("okapi_service", labelValue).watch(this);
  }

  private String effectiveNamespace() {
    String ns = properties.getNamespace();
    if (ns != null && !ns.isBlank()) return ns;
    try {
      return Files.readString(Path.of("/var/run/secrets/kubernetes.io/serviceaccount/namespace"))
          .trim();
    } catch (IOException e) {
      return "default";
    }
  }

  @Override
  public void eventReceived(Action action, Pod pod) {
    String podUid = Optional.ofNullable(pod.getMetadata()).map(m -> m.getUid()).orElse(null);
    String podIp = Optional.ofNullable(pod.getStatus()).map(s -> s.getPodIP()).orElse(null);
    if (podUid == null) return;

    switch (action) {
      case ADDED, MODIFIED -> {
        boolean ready = properties.isIncludeNotReady() || K8sSeedRegistry_isReady(pod);
        if (!ready) return;
        if (podIp == null || podIp.equals(whoAmI.getNodeIp())) return;
        // if not cached, resolve meta and add
        var entry = registry.getCache().get(podUid);
        if (entry == null || !Objects.equals(entry.ip, podIp)) {
          String nodeId = resolveNodeId(podIp);
          if (nodeId == null || nodeId.isBlank()) return;
          var e = new K8sSeedRegistry.Entry(nodeId, podIp, serverPort);
          registry.getCache().put(podUid, e);
          memberList.addMember(new Member(nodeId, podIp, serverPort));
        }
      }
      case DELETED -> {
        var entry = registry.getCache().remove(podUid);
        if (entry != null) {
          memberList.remove(entry.nodeId);
        }
      }
      case ERROR -> {
        /* no-op; will handle via onClose */
      }
    }
  }

  private boolean K8sSeedRegistry_isReady(Pod p) {
    if (p.getStatus() == null || p.getStatus().getConditions() == null) return false;
    return p.getStatus().getConditions().stream()
        .anyMatch(
            c -> Objects.equals(c.getType(), "Ready") && Objects.equals(c.getStatus(), "True"));
  }

  private String resolveNodeId(String ip) {
    return registry.resolveNodeId(ip);
  }

  @Override
  public void onClose(WatcherException cause) {
    long backoff = properties.getWatchReconnectInitialBackoffMillis();
    reconnectWithBackoff(backoff);
  }
}
