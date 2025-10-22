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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.okapi.swim.bootstrap.SeedMembersProvider;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.ping.Member;
import org.okapi.swim.rest.MetaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("k8s")
@ConditionalOnClass(KubernetesClient.class)
@RequiredArgsConstructor
public class K8sSeedRegistry implements SeedMembersProvider {

  private final KubernetesClient client;
  private final K8sDiscoveryProperties properties;
  private final OkHttpClient httpClient;
  private final Gson gson;
  private final WhoAmI whoAmI;

  // Cache of podUid -> nodeId (and last known ip)
  @AllArgsConstructor
  static class Entry {
    String nodeId;
    String ip;
    int port;
  }

  private final Map<String, Entry> cache = new ConcurrentHashMap<>();

  @Value("${server.port}")
  private int serverPort;

  @Override
  public List<Member> getSeedMembers() {
    String ns = effectiveNamespace();
    String labelValue = properties.getOkapiServiceLabelValue();
    if (labelValue == null || labelValue.isBlank()) {
      return List.of();
    }
    var pods =
        client.pods().inNamespace(ns).withLabel("okapi_service", labelValue).list().getItems();
    List<Member> result = new ArrayList<>();
    for (Pod p : pods) {
      if (!properties.isIncludeNotReady() && !isReady(p)) continue;
      String podIp = Optional.ofNullable(p.getStatus()).map(PodStatus::getPodIP).orElse(null);
      String podUid = Optional.ofNullable(p.getMetadata()).map(ObjectMeta::getUid).orElse(null);
      if (podIp == null || podUid == null) continue;
      if (podIp.equals(whoAmI.getNodeIp())) continue; // exclude self by IP
      String nodeId = resolveNodeId(podIp);
      if (nodeId == null || nodeId.isBlank()) continue;
      result.add(new Member(nodeId, podIp, serverPort));
      cache.put(podUid, new Entry(nodeId, podIp, serverPort));
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

  String resolveNodeId(String ip) {
    var req =
        new Request.Builder()
            .url("http://" + ip + ":" + serverPort + "/okapi/swim/meta")
            .get()
            .build();
    try (var call = httpClient.newCall(req).execute()) {
      if (call.code() >= 200 && call.code() < 300 && call.body() != null) {
        var body = call.body().string();
        MetaResponse meta = gson.fromJson(body, MetaResponse.class);
        return meta != null ? meta.getNodeId() : null;
      }
    } catch (IOException ignored) {
    }
    return null;
  }

  Map<String, Entry> getCache() {
    return cache;
  }
}
