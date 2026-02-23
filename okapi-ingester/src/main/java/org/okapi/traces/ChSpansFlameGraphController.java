/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces;

import org.okapi.rest.traces.SpanQueryV2Request;
import org.okapi.rest.traces.SpansFlameGraphResponse;
import org.okapi.traces.ch.ChSpansFlameGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/spans")
public class ChSpansFlameGraphController {

  @Autowired ChSpansFlameGraphService flameGraphService;

  @PostMapping("/flamegraph")
  public SpansFlameGraphResponse getFlameGraph(@RequestBody SpanQueryV2Request request) {
    return flameGraphService.queryFlameGraph(request);
  }
}
