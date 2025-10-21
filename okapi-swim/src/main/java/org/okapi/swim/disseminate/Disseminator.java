package org.okapi.swim.disseminate;

import com.google.gson.Gson;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.okapi.swim.HttpException;
import org.okapi.swim.Result;
import org.okapi.swim.config.SwimConfig;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.ping.Member;
import org.okapi.swim.ping.MemberList;
import org.okapi.swim.rest.AckMessage;
import org.okapi.swim.rest.AliveMessage;
import org.okapi.swim.rest.RegisterMessage;
import org.okapi.swim.rest.SuspectMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Disseminator {
  private final MemberList memberList;
  private final OkHttpClient httpClient;
  private final WhoAmI whoAmI;
  private final ExecutorService executorService;
  private final Gson gson;
  private final SwimConfig swimConfig;

  public Result<AckMessage, Exception> disseminateUnHealthy(String nodeId) {
    for (Member member : memberList.getAllMembers()) {
      if (member.getNodeId().equals(nodeId) || member.getNodeId().equals(whoAmI.getNodeId())) {
        continue;
      }

      var url = "http://" + member.getIp() + ":" + member.getPort() + "/okapi/swim/" + nodeId;
      var req = new Request.Builder().url(url).delete().build();
      try (var call = httpClient.newCall(req).execute()) {
        int code = call.code();
        if (code < 200 || code >= 300) {
          String body = call.body() != null ? call.body().string() : null;
          return new Result<>(null, new HttpException(code, body));
        }
      } catch (IOException e) {
        return new Result<>(null, e);
      }
    }
    return new Result<>(new AckMessage(), null);
  }

  public static class BroadcastResult {
    public final int successCount;
    public final int failureCount;
    public final boolean quorumMet;

    public BroadcastResult(int successCount, int failureCount, boolean quorumMet) {
      this.successCount = successCount;
      this.failureCount = failureCount;
      this.quorumMet = quorumMet;
    }
  }

  private List<Member> samplePeersExcluding(String subjectNodeId) {
    Set<String> exclude = new HashSet<>();
    exclude.add(subjectNodeId);
    exclude.add(whoAmI.getNodeId());
    int k = swimConfig.getK() > 0 ? swimConfig.getK() : 1;
    return memberList.sampleKExcluding(exclude, k);
  }

  private BroadcastResult broadcastJson(String pathSuffix, Object body) {
    List<Member> peers = samplePeersExcluding(extractNodeId(body));
    if (peers == null) {
      return new BroadcastResult(0, 0, true);
    }
    int sampledSize = peers.size();
    if (sampledSize == 0) {
      return new BroadcastResult(0, 0, true);
    }
    int m = swimConfig.getMQuorum() > 0 ? Math.min(swimConfig.getMQuorum(), sampledSize) : sampledSize;
    long timeoutMs = swimConfig.getBroadcastTimeoutMillis() > 0 ? swimConfig.getBroadcastTimeoutMillis() : 5000;

    List<Future<Boolean>> futures = new ArrayList<>();
    for (Member peer : peers) {
      futures.add(
          executorService.submit(
              () -> {
                var url = "http://" + peer.getIp() + ":" + peer.getPort() + pathSuffix;
                var reqBody = RequestBody.create(MediaType.parse("application/json"), gson.toJson(body).getBytes());
                var req = new Request.Builder().url(url).put(reqBody).build();
                try (var call = httpClient.newCall(req).execute()) {
                  return call.code() >= 200 && call.code() < 300;
                } catch (IOException e) {
                  return false;
                }
              }));
    }
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
    int success = 0, failures = 0;
    for (Future<Boolean> f : futures) {
      long remaining = deadline - System.nanoTime();
      if (remaining <= 0) break;
      try {
        boolean ok = f.get(remaining, TimeUnit.NANOSECONDS);
        if (ok) success++; else failures++;
        if (success >= m) {
          // Optionally cancel remaining tasks
          for (Future<Boolean> other : futures) other.cancel(true);
          return new BroadcastResult(success, failures, true);
        }
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        failures++;
      }
    }
    return new BroadcastResult(success, failures, success >= m);
  }

  private static String extractNodeId(Object msg) {
    if (msg instanceof RegisterMessage r) return r.getNodeId();
    if (msg instanceof AliveMessage a) return a.getNodeId();
    if (msg instanceof SuspectMessage s) return s.getNodeId();
    return "";
  }

  public BroadcastResult disseminateRegister(RegisterMessage msg) {
    String path = "/okapi/swim/members/" + msg.getNodeId();
    return broadcastJson(path, msg);
  }

  public BroadcastResult disseminateAlive(AliveMessage msg) {
    String path = "/okapi/swim/members/" + msg.getNodeId() + "/alive";
    return broadcastJson(path, msg);
  }

  public BroadcastResult disseminateSuspect(SuspectMessage msg) {
    String path = "/okapi/swim/members/" + msg.getNodeId() + "/suspect";
    return broadcastJson(path, msg);
  }
}
