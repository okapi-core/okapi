package org.okapi.traces.api;

import org.okapi.exceptions.BadRequestException;
import org.okapi.rest.traces.SpansQueryStatsRequest;
import org.okapi.rest.traces.SpansQueryStatsResponse;
import org.okapi.traces.ch.ChSpanStatsQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChSpanStatsController {

  @Autowired private ChSpanStatsQueryService statsQueryService;

  @PostMapping("/spans/stats")
  public SpansQueryStatsResponse getStats(@RequestBody SpansQueryStatsRequest request)
      throws BadRequestException {
    return statsQueryService.getStats(request);
  }
}
