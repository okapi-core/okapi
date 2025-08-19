package org.okapi.staticserver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
  @Getter
  int code;
  T data;
  String error;

  public boolean isSuccess() {
    return code >= 200 && code < 300;
  }

  public boolean isError() {
    return !isSuccess();
  }

  public Optional<T> getData() {
    return Optional.ofNullable(data);
  }

  public Optional<String> getError() {
    return Optional.ofNullable(error);
  }
}
