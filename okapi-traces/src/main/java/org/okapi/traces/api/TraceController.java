package org.okapi.traces.api;

import org.okapi.traces.model.Span;
import org.okapi.traces.service.TraceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/traces")
public class TraceController {

    @Autowired
    private TraceService traceService;

    // Endpoint to retrieve spans for a given trace id
    @GetMapping("/{traceId}/spans")
    public List<Span> getSpans(@PathVariable String traceId,
                               @RequestHeader("X-Okapi-Tenant") String tenant,
                               @RequestHeader("X-Okapi-App") String app) {
        return traceService.getSpans(traceId, tenant, app);
    }

    // Future endpoints for ingesting OTel trace requests can be added here.
}
