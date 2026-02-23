package org.okapi.web.service.query;

import com.google.gson.Gson;
import java.io.IOException;
import okhttp3.Response;
import org.okapi.exceptions.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class ProxyResponseTranslator {
  Gson gson = new Gson();

  public <T> T translateResponse(Response response, Class<T> clazz) throws IOException {
    if (response.isSuccessful()) {
      var resBody = response.body().string();
      return gson.fromJson(resBody, clazz);
    } else if (response.code() == 400) {
      throw new BadRequestException(response.body().string());
    } else {
      throw new DownstreamFailedException();
    }
  }
}
