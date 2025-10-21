package org.okapi.swim.ping;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.okapi.swim.HttpException;
import org.okapi.swim.Result;
import org.okapi.swim.config.SwimConfig;
// import removed
import org.okapi.swim.disseminate.Disseminator;
import org.okapi.swim.membership.MembershipService;
import org.okapi.swim.rest.AckMessage;
import org.okapi.swim.rest.AliveMessage;
import org.okapi.swim.rest.PingMessage;
import org.okapi.swim.rest.PingRequest;
import org.okapi.swim.rest.SuspectMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PingService {

  private final MemberList memberList;
  private final SwimConfig swimConfig;
  private final Gson gson;
  private final OkHttpClient httpClient;
  private final ExecutorService executorService;
  private final MembershipService membershipService;
  private final Disseminator disseminator;

  public Optional<AckMessage> pingKIndirect(String nodeId)
      throws ExecutionException, InterruptedException, TimeoutException {
    var kSample = memberList.sampleK(swimConfig.getK());
    var futures = new ArrayList<Future<Result<AckMessage, Exception>>>();
    for (var member : kSample) {
      futures.add(executorService.submit(() -> pingMemberIndirect(member, nodeId)));
    }
    for (var future : futures) {
      var result = future.get(swimConfig.getTimeoutMillis(), TimeUnit.MILLISECONDS);
      if (result.getResult() != null) {
        return Optional.of(result.getResult());
      }
    }
    return Optional.empty();
  }

  public Result<AckMessage, Exception> pingMemberIndirect(Member member, String nodeId) {
    var pingRequest = new PingRequest(nodeId);
    var requestBody =
        RequestBody.create(
            MediaType.parse("application/json"), gson.toJson(pingRequest).getBytes());
    var req =
        new Request.Builder()
            .url("http://" + member.getIp() + ":" + member.getPort() + "/okapi/swim/ping-indirect")
            .post(requestBody)
            .build();
    try (var call = httpClient.newCall(req).execute()) {
      int code = call.code();
      boolean ok = code >= 200 && code < 300;
      if (ok) {
        if (call.body() != null) {
          var response = call.body().string();
          var msg = gson.fromJson(response, AckMessage.class);
          membershipService.applyAlive(member.getNodeId(), 0L);
          disseminator.disseminateAlive(
              new AliveMessage(member.getNodeId(), 0L, swimConfig.getGossipHopCount()));
          return new Result<>(msg, null);
        } else {
          return new Result<>(null, new HttpException(code, null));
        }
      } else {
        String body = call.body() != null ? call.body().string() : null;
        membershipService.applySuspect(
            member.getNodeId(), 0L, swimConfig.getSuspectTimeoutMillis());
        disseminator.disseminateSuspect(
            new SuspectMessage(member.getNodeId(), 0L, swimConfig.getGossipHopCount()));
        return new Result<>(null, new HttpException(code, body));
      }
    } catch (IOException e) {
      return new Result<>(null, e);
    }
  }

  public Result<AckMessage, Exception> ping(String nodeId) {
    var member = memberList.getMember(nodeId);
    var pingmsg = new PingMessage(member.getNodeId(), null, 0);
    var requestBody =
        RequestBody.create(MediaType.parse("application/json"), gson.toJson(pingmsg).getBytes());
    var req =
        new Request.Builder()
            .url("http://" + member.getIp() + ":" + member.getPort() + "/okapi/swim/ping")
            .post(requestBody)
            .build();
    try (var call = httpClient.newCall(req).execute()) {
      int code = call.code();
      boolean ok = code >= 200 && code < 300;
      if (ok) {
        if (call.body() != null) {
          var response = call.body().string();
          var msg = gson.fromJson(response, AckMessage.class);
          membershipService.applyAlive(member.getNodeId(), 0L);
          disseminator.disseminateAlive(
              new AliveMessage(member.getNodeId(), 0L, swimConfig.getGossipHopCount()));
          return new Result<>(msg, null);
        } else {
          return new Result<>(null, new HttpException(code, null));
        }
      } else {
        String body = call.body() != null ? call.body().string() : null;
        membershipService.applySuspect(
            member.getNodeId(), 0L, swimConfig.getSuspectTimeoutMillis());
        disseminator.disseminateSuspect(
            new SuspectMessage(member.getNodeId(), 0L, swimConfig.getGossipHopCount()));
        return new Result<>(null, new HttpException(code, body));
      }
    } catch (IOException e) {
      return new Result<>(null, e);
    }
  }
}
