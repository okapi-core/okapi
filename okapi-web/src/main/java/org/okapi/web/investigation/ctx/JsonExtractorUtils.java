package org.okapi.web.investigation.ctx;

import org.okapi.json.JsonExtractor;
import org.okapi.web.ai.provider.ApiResponse;

public class JsonExtractorUtils {

    public static JsonExtractor getJsonExtractor(ApiResponse response) {
        var json = response.getResponse();
        return new JsonExtractor(json);
    }

}
