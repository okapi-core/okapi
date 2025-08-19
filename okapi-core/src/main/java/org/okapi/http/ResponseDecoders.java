package org.okapi.http;

import static org.okapi.validation.OkapiResponseChecks.is4xx;
import static org.okapi.validation.OkapiResponseChecks.is5xx;

import com.google.gson.Gson;
import org.okapi.exceptions.BadRequestException;
import org.okapi.exceptions.InternalFailureException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

@Slf4j
public class ResponseDecoders {

  public static void translate4xx(Response response) throws IOException, BadRequestException {
    if (is4xx(response.code())) {
      if (response.body() != null) {
        throw new BadRequestException(response.body().string());
      } else throw new BadRequestException();
    }
  }

  public static void translate5xx(Response response) throws InternalFailureException {
    if (is5xx(response.code())) {
      throw new InternalFailureException();
    }
  }

  public static <T> T translateResponse(Response response, Class<T> clazz, Gson gson)
      throws InternalFailureException, IOException, BadRequestException {
    translate5xx(response);
    translate4xx(response);
    var body = response.body();
    if (body == null) {
      throw new InternalFailureException();
    }
    return gson.fromJson(body.string(), clazz);
  }

  public static void translateResponse(Response response)
      throws InternalFailureException, IOException, BadRequestException {
    translate5xx(response);
    translate4xx(response);
  }
  

}
