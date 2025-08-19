package org.okapi.staticserver;

import org.okapi.staticserver.exceptions.BadRequestException;
import java.io.IOException;
import okhttp3.Response;

public class ResponseEntityBuilder {

  public static void handleError(Response response) throws BadRequestException, IOException {
    if (response.code() >= 400 && response.code() <= 499) {
      throw new BadRequestException(response.code(), response.body().string());
    }
  }
}
