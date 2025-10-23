package org.okapi.swim.membership;

import com.google.gson.Gson;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.swim.identity.WhoAmI;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RequiredArgsConstructor
@Slf4j
public class S3MembershipEventPublisher implements MembershipEventPublisher {
  private final SwimMembershipProperties props;
  private final S3Client s3;
  private final Gson gson;

  @Override
  public void emitPodAdd(WhoAmI whoAmI, long timestampMillis) {
    emit("pod_add", whoAmI, timestampMillis);
  }

  @Override
  public void emitPodDelete(WhoAmI whoAmI, long timestampMillis) {
    emit("pod_delete", whoAmI, timestampMillis);
  }

  private void emit(String type, WhoAmI whoAmI, long ts) {
    try {
      String bucket = props.getMembershipS3Bucket();
      if (bucket == null || bucket.isBlank()) return; // disabled
      String prefix = buildPrefix(props.getServiceName(), ts);
      String key = prefix + "/events-" + ts + "-" + UUID.randomUUID();

      Map<String, Object> event =
          Map.of(
              "type", type,
              "nodeId", whoAmI.getNodeId(),
              "ip", whoAmI.getNodeIp(),
              "port", whoAmI.getNodePort(),
              "timestampMillis", ts);

      byte[] bytes = (gson.toJson(event) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
      s3.putObject(
          PutObjectRequest.builder().bucket(bucket).key(key).build(), RequestBody.fromBytes(bytes));
    } catch (Exception e) {
      log.warn("Failed to emit membership event {}", type, e);
    }
  }

  private static String buildPrefix(String serviceName, long tsMillis) {
    ZonedDateTime z = Instant.ofEpochMilli(tsMillis).atZone(ZoneOffset.UTC);
    return serviceName
        + "/membership/"
        + String.format(
            "%04d/%02d/%02d/%02d", z.getYear(), z.getMonthValue(), z.getDayOfMonth(), z.getHour());
  }
}
