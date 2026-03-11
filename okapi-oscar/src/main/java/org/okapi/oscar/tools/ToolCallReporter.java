package org.okapi.oscar.tools;

import com.google.gson.Gson;
import org.springframework.stereotype.Component;

@Component
public class ToolCallReporter {

  private static final ThreadLocal<StatefulTools> CONTEXT = new ThreadLocal<>();
  private static final Gson GSON = new Gson();

  public Scope withStatefulTools(StatefulTools statefulTools) {
    return new Scope(statefulTools);
  }

  public void reportRequest(String toolName, Object request, String summary) {
    var tools = CONTEXT.get();
    if (tools == null) {
      return;
    }
    String json = request == null ? "null" : GSON.toJson(request);
    tools.postToolCallRequest(toolName, json, summary);
  }

  public void reportResponse(String toolName, Object response, String summary) {
    var tools = CONTEXT.get();
    if (tools == null) {
      return;
    }
    String json = response == null ? "null" : GSON.toJson(response);
    tools.postToolCallResponse(toolName, json, summary);
  }

  public static class Scope implements AutoCloseable {
    private final StatefulTools prior;
    private boolean closed;

    private Scope(StatefulTools statefulTools) {
      this.prior = CONTEXT.get();
      CONTEXT.set(statefulTools);
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      closed = true;
      if (prior == null) {
        CONTEXT.remove();
      } else {
        CONTEXT.set(prior);
      }
    }
  }
}
