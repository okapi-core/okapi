package org.okapi.metrics.cas;

import java.io.IOException;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.service.web.QueryProcessor;
import org.okapi.rest.metrics.*;
import org.rocksdb.RocksDBException;

public class CasQueryProcessor implements QueryProcessor {
    @Override
    public GetMetricsResponse getMetricsResponse(GetMetricsRequestInternal request) throws Exception {
        return null;
    }

    @Override
    public SearchMetricsResponse searchMetricsResponse(SearchMetricsRequestInternal searchMetricsRequest) throws BadRequestException, RocksDBException, IOException {
        return null;
    }

    @Override
    public ListMetricsResponse listMetricsResponse(ListMetricsRequest request) {
        return null;
    }
}
