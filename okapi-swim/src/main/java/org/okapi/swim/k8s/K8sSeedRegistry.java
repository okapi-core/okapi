package org.okapi.swim.k8s;

import com.google.gson.Gson;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.okapi.swim.bootstrap.SeedMembersProvider;
import org.okapi.swim.config.SwimConfiguration;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.ping.Member;
import org.okapi.swim.rest.MetaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("k8s")
public class K8sSeedRegistry implements SeedMembersProvider {

  public static final String LABEL = "okapi_service";
  public static final Integer IP_RESOLVE_RETRIES = 4;
  public static final Integer WAIT_TIME_MILLIS = 2000; // wait 2 seconds before retrying
  private final KubernetesClient client;
  private final K8sDiscoveryProperties properties;
  private final OkHttpClient httpClient;
  private final Gson gson;
  private final WhoAmI whoAmI;
  private int serverPort;

  private final Map<String, Entry> cache = new ConcurrentHashMap<>();

  public K8sSeedRegistry(
      @Autowired KubernetesClient client,
      @Autowired K8sDiscoveryProperties properties,
      @Autowired @Qualifier(SwimConfiguration.SWIM_OK_HTTP) OkHttpClient httpClient,
      @Autowired WhoAmI whoAmI,
      @Value("${server.port}") int serverPort) {
    this.client = client;
    this.properties = properties;
    this.httpClient = httpClient;
    this.gson = new Gson();
    this.whoAmI = whoAmI;
    this.serverPort = serverPort;
  }

  // Cache of podUid -> nodeId (and last known ip)
  @AllArgsConstructor
  static class Entry {
    String nodeId;
    String ip;
  }

  @Override
  public List<Member> getSeedMembers() throws InterruptedException {
    log.info("Getting seed members.");
    String ns = effectiveNamespace();
    String labelValue = properties.getSvcName();

    if (labelValue == null) {
      log.error("Service label value {}", labelValue);
      throw new RuntimeException("A service value must be supplied when running k8s profile.");
    }

    List<Pod> pods = client.pods().inNamespace(ns).withLabel(LABEL, labelValue).list().getItems();
    log.info("Found {} pods with label: {}={} ", pods.size(), LABEL, labelValue);
    List<Member> result = new ArrayList<>();
    for (Pod p : pods) {
      if (!properties.isIncludeNotReady() && !isReady(p)) {
        log.info("Pod {} is not ready so moving on.", p.getMetadata().getUid());
        continue;
      }
      String podIp = Optional.ofNullable(p.getStatus()).map(PodStatus::getPodIP).orElse(null);
      String podUid = Optional.ofNullable(p.getMetadata()).map(ObjectMeta::getUid).orElse(null);
      if (podIp == null || podUid == null || podIp.equals(whoAmI.getNodeIp())) {
        log.info(
            "Found pod with podIp = {} , podUid = {}, whoAmI = {}, so moving on.",
            podIp,
            podUid,
            podIp);
        continue;
      }

      // resolve the node
      log.info("Resolving nodeid for pod: {}", podIp);
      String nodeId = resolveNodeId(podIp);
      if (nodeId == null) {
        log.info("Node id is null for pod with ip: {}", podIp);
        continue;
      }
      if (nodeId.isBlank()) {
        log.info("Node id is blank for pod with ip: {}", podIp);
        continue;
      }
      log.info("Adding pod: {} as member.", podIp);
      result.add(new Member(nodeId, podIp, serverPort));
      cache.put(podUid, new Entry(nodeId, podIp));
    }
    return result;
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

  private static boolean isReady(Pod p) {
    if (p.getStatus() == null || p.getStatus().getConditions() == null) return false;
    for (PodCondition c : p.getStatus().getConditions()) {
      if (Objects.equals(c.getType(), "Ready") && Objects.equals(c.getStatus(), "True"))
        return true;
    }
    return false;
  }

  String resolveNodeId(String ip) throws InterruptedException {
    for (int i = 0; i < IP_RESOLVE_RETRIES; i++) {
      log.info("Try: {} Calling ip: {}", i, ip);
      var req =
          new Request.Builder()
              .url("http://" + ip + ":" + serverPort + "/okapi/swim/meta")
              .get()
              .build();
      try (var call = httpClient.newCall(req).execute()) {
        log.info("Got response: {}", call.code());
        if (call.code() >= 200 && call.code() < 300 && call.body() != null) {
          var body = call.body().string();
          log.info("Got response body: {}", body);
          MetaResponse meta = gson.fromJson(body, MetaResponse.class);
          log.info("Got meta response: {}", meta.getIAm());
          return meta.getIAm();
        }
      } catch (IOException ignored) {
        log.error("Could not contact ip: {}", ip);
        log.error("Failed with exception: {}", ignored);
      }
      Thread.sleep(WAIT_TIME_MILLIS);
    }
    return null;
  }

  Map<String, Entry> getCache() {
    return cache;
  }
}
