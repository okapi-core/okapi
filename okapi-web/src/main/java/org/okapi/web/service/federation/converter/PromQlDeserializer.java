package org.okapi.web.service.federation.converter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import org.okapi.agent.dto.results.promql.*;

public class PromQlDeserializer {
  Gson gson = new Gson();

  public PromQlResponse<PromQlData<PromQlRangeResult>> getRangeResult(String json) {
    Type type = new TypeToken<PromQlResponse<PromQlData<PromQlRangeResult>>>() {}.getType();
    return gson.fromJson(json, type);
  }

  public PromQlResponse<PromQlData<PromQlInstantResult>> getInstantResult(String json) {
    Type type = new TypeToken<PromQlResponse<PromQlData<PromQlInstantResult>>>() {}.getType();
    return gson.fromJson(json, type);
  }

  public PromQlArrayResponse<String> getLabelsResult(String json) {
    Type type = new TypeToken<PromQlArrayResponse<String>>() {}.getType();
    return gson.fromJson(json, type);
  }
}
