package org.okapi.oscar.integ;

import com.google.gson.Gson;
import java.util.List;
import org.junit.jupiter.api.TestInstance;
import org.okapi.ingester.client.IngesterClient;
import org.okapi.oscar.integ.judge.JudgeAgent;
import org.okapi.oscar.service.OscarAi;
import org.okapi.rest.chat.CHAT_RESPONSE_TYPE;
import org.okapi.rest.chat.ChatMessageResponse;
import org.okapi.rest.chat.PostMessageRequest;
import org.okapi.rest.chat.payload.PostResponsePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class OscarIntegTestBase {

  protected static final Logger log = LoggerFactory.getLogger(OscarIntegTestBase.class);
  protected static final String TEST_USER = "test-user";
  private static final Gson GSON = new Gson();

  @Autowired protected OscarAi oscarAi;
  @Autowired protected JudgeAgent judgeAgent;
  @Autowired protected IngesterClient ingesterClient;

  protected String session() {
    return oscarAi.createSession().getSessionId();
  }

  protected PostMessageRequest msg(String message) {
    return PostMessageRequest.builder().message(message).userId(TEST_USER).build();
  }

  protected String getLatest(String sessionId) {
    List<ChatMessageResponse> responses = oscarAi.getHistory(sessionId, 0L, null).getResponses();
    ChatMessageResponse latest =
        responses.stream()
            .filter(r -> r.getResponseType() == CHAT_RESPONSE_TYPE.RESPONSE)
            .reduce((a, b) -> b)
            .orElseThrow(() -> new IllegalStateException("No RESPONSE message found in session " + sessionId));
    String responseText = GSON.fromJson(latest.getContents(), PostResponsePayload.class).response();
    log.info("[session={}] Latest AI response:\n{}", sessionId, responseText);
    return responseText;
  }
}
