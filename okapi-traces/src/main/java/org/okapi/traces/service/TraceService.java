package org.okapi.traces.service;

import org.okapi.traces.model.Span;
import org.okapi.traces.sampler.SamplingStrategy;
import org.okapi.traces.storage.TraceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TraceService {

    @Autowired
    private TraceRepository traceRepository;

    @Autowired
    private SamplingStrategy samplingStrategy;

    public List<Span> getSpans(String traceId, String tenant, String app) {
        // Apply sampling strategy; if the trace is not sampled, return empty.
        if (!samplingStrategy.sample(traceId)) {
            return List.of();
        }
        return traceRepository.getSpansByTraceId(traceId, tenant, app);
    }

    // Future methods for trace ingestion can be added here.
}
