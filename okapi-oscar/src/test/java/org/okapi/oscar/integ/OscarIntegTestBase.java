package org.okapi.oscar.integ;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.TestInstance;
import org.okapi.ingester.client.IngesterClient;
import org.okapi.oscar.integ.judge.JudgeAgent;
import org.okapi.oscar.service.OscarAi;
import org.okapi.rest.chat.ChatResponse;
import org.okapi.rest.chat.PostMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class OscarIntegTestBase {

  protected static final Logger log = LoggerFactory.getLogger(OscarIntegTestBase.class);
  protected static final String TEST_USER = "test-user";

  @Autowired protected OscarAi oscarAi;
  @Autowired protected JudgeAgent judgeAgent;
  @Autowired protected IngesterClient ingesterClient;

  protected String session() {
    return UUID.randomUUID().toString();
  }

  protected PostMessageRequest msg(String message) {
    return PostMessageRequest.builder().message(message).userId(TEST_USER).build();
  }

  protected String getLatest(String sessionId) {
    List<ChatResponse> responses = oscarAi.getHistory(sessionId, 0L, null).getResponses();
    ChatResponse latest = responses.get(responses.size() - 1);
    log.info("[session={}] Latest AI response:\n{}", sessionId, latest.getContents());
    return latest.getContents();
  }
}
