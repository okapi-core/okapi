package org.okapi.logs.api;

import org.okapi.logs.LogsBufferPool;
import org.okapi.logs.TestApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("test")
class OtelLogsControllerBulkIngestTest {

  @Autowired OtelLogsController controller;
  @Autowired LogsBufferPool bufferPool;
}
