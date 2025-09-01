package org.okapi.metrics.query.promql;

import java.util.*;
import org.okapi.promql.eval.*;
import org.okapi.rest.promql.*;

public class PromToResponseMapper {

  public static final String NAME = "__name__";

  public enum RETURN_TYPE {
    VECTOR,
    MATRIX,
  }

  public static GetPromQlResponse<PromQlData<?>> toResult(
      ExpressionResult result, RETURN_TYPE returnType) {
    if (returnType == RETURN_TYPE.VECTOR) {
      var response = new GetPromQlResponse<PromQlData<?>>();
      var data = mapInstanceVector(result);
      response.setStatus("success");
      response.setData(data);
      return response;
    } else if (returnType == RETURN_TYPE.MATRIX) {
      var response = new GetPromQlResponse<PromQlData<?>>();
      var data = mapMatrixSeries(result);
      response.setStatus("success");
      response.setData(data);
      return response;
    }
    throw new IllegalStateException();
  }

  public static PromQlData<List<VectorSeries>> mapInstanceVector(ExpressionResult result) {
    var iv = (InstantVectorResult) result;
    var promQlResponse = new PromQlData<List<VectorSeries>>();
    promQlResponse.setResultType(PromQlResultType.MATRIX);
    List<VectorSeries> asVectorSeriesList =
        iv.data().stream()
            .map(
                seriesSample -> {
                  var metric = toPrometheusName(seriesSample.id());
                  var sample = fromEngineSampleToRestSample(seriesSample.sample());
                  var series = new VectorSeries(metric, sample);
                  return series;
                })
            .toList();
    promQlResponse.setResultType(PromQlResultType.VECTOR);
    promQlResponse.setResult(asVectorSeriesList);
    return promQlResponse;
  }

  public static PromQlData<List<MatrixSeries>> mapMatrixSeries(ExpressionResult result) {
    var asIv = (InstantVectorResult) result;
    var asMat = asIv.toMatrix();
    var promQlData = new PromQlData<List<MatrixSeries>>();
    List<MatrixSeries> asMatSeries =
        asMat.entrySet().stream()
            .map(
                w -> {
                  var toName = toPrometheusName(w.getKey());
                  var values =
                      w.getValue().stream().map(PromToResponseMapper::fromEngineSampleToRestSample).toList();
                  return new MatrixSeries(toName, values);
                })
            .toList();
    promQlData.setResult(asMatSeries);
    return promQlData;
  }

  public static Sample fromEngineSampleToRestSample(VectorData.Sample engineSample) {
    var inSecond = engineSample.ts() / 1000.f;
    var strVal = "" + engineSample.value();
    return new Sample(inSecond, strVal);
  }

  public static Map<String, String> toPrometheusName(VectorData.SeriesId seriesId) {
    return Collections.unmodifiableMap(
        new HashMap<>(seriesId.labels().tags()) {
          {
            put(NAME, seriesId.metric());
          }
        });
  }
}
