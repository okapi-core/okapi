package org.okapi.swim.membership;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.swim.ping.Member;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(S3Client.class)
@RequiredArgsConstructor
@Slf4j
public class S3TimelineBuilder implements TimelineBuilder {
  private final SwimMembershipProperties props;
  private final S3Client s3;
  private final Gson gson;

  @Override
  public Set<Member> buildAliveSetForHour(long hourStartMillis) {
    String bucket = props.getMembershipS3Bucket();
    if (bucket == null || bucket.isBlank()) return Set.of();
    String prefix = hourPrefix(props.getServiceName(), hourStartMillis);

    Map<String, Member> alive = new HashMap<>();
    try {
      var req = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix + "/").build();
      var resp = s3.listObjectsV2(req);
      if (resp.contents() == null) return Set.of();
      for (var obj : resp.contents()) {
        byte[] bytes =
            s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(obj.key()).build())
                .asByteArray();
        // event per object; tolerate multiple newline-terminated jsons
        String content = new String(bytes, StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) continue;
        for (String line : content.split("\n")) {
          JsonObject ev = gson.fromJson(line, JsonObject.class);
          String type = ev.get("type").getAsString();
          String nodeId = ev.get("nodeId").getAsString();
          String ip = ev.get("ip").getAsString();
          int port = ev.get("port").getAsInt();
          if ("pod_add".equals(type)) {
            alive.put(nodeId, new Member(nodeId, ip, port));
          } else if ("pod_delete".equals(type)) {
            alive.remove(nodeId);
          }
        }
      }
    } catch (Exception e) {
      log.warn("Failed to build alive set for {}", prefix, e);
    }
    return new HashSet<>(alive.values());
  }

  private static String hourPrefix(String serviceName, long tsMillis) {
    ZonedDateTime z = Instant.ofEpochMilli(tsMillis).atZone(ZoneOffset.UTC);
    return serviceName + "/membership/" +
        String.format("%04d/%02d/%02d/%02d", z.getYear(), z.getMonthValue(), z.getDayOfMonth(), z.getHour());
  }
}
