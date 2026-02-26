package org.okapi.futures;

public record Result<T, E extends RuntimeException>(T result, E error) {
  public static <T, E extends RuntimeException> Result<T, E> ofError(E err) {
    return new Result<>(null, err);
  }

  public static <T, E extends RuntimeException> Result<T, E> ofValue(T value) {
    return new Result<>(value, null);
  }
}
