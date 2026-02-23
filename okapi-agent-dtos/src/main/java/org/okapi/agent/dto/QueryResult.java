package org.okapi.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode
public class QueryResult {
  String data;
  String processingError;
  String error;

  public static QueryResult ofData(String data) {
    return QueryResult.builder().data(data).build();
  }

  public static QueryResult ofError(String error) {
    return QueryResult.builder().error(error).build();
  }

  public static QueryResult ofProcessingError(String err) {
    return QueryResult.builder().processingError(err).build();
  }

  public String data() {
    return data;
  }

  public String processingError() {
    return processingError;
  }

  public String error() {
    return error;
  }
}
