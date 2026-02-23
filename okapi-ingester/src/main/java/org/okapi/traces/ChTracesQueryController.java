package org.okapi.traces;

import org.okapi.rest.traces.SpanQueryV2Request;
import org.okapi.rest.traces.SpanQueryV2Response;
import org.okapi.traces.ch.ChTraceQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChTracesQueryController {

  @Autowired ChTraceQueryService traceQueryService;

  @PostMapping("/spans/query")
  public SpanQueryV2Response getSpans(@RequestBody SpanQueryV2Request requestV2) {
    return traceQueryService.getSpans(requestV2);
  }
}
