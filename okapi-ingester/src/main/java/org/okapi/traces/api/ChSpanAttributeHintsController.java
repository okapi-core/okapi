package org.okapi.traces.api;

import org.okapi.rest.traces.SpanAttributeHintsRequest;
import org.okapi.rest.traces.SpanAttributeHintsResponse;
import org.okapi.rest.traces.SpanAttributeValueHintsRequest;
import org.okapi.rest.traces.SpanAttributeValueHintsResponse;
import org.okapi.traces.ch.ChSpanAttributeHintsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChSpanAttributeHintsController {

  @Autowired private ChSpanAttributeHintsService hintsService;

  @PostMapping("/spans/attributes/hints")
  public SpanAttributeHintsResponse getAttributeHints(
      @RequestBody SpanAttributeHintsRequest request) {
    return hintsService.getAttributeHints(request);
  }

  @PostMapping("/spans/attributes/values/hints")
  public SpanAttributeValueHintsResponse getAttributeValueHints(
      @RequestBody SpanAttributeValueHintsRequest request) {
    return hintsService.getAttributeValueHints(request);
  }
}
