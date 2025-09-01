package org.okapi.rest.promql;

import lombok.Data;
import com.google.gson.annotations.SerializedName;

/**
 * Wraps resultType and result payload for a Prometheus query.
 * ResultT is the type matching resultType:
 *   - List<VectorSeries> for VECTOR
 *   - List<MatrixSeries> for MATRIX
 *   - ScalarResult for SCALAR
 *   - StringResult for STRING
 */
@Data
public class PromQlData<ResultT> {
    @SerializedName("resultType")
    private PromQlResultType resultType;

    @SerializedName("result")
    private ResultT result;
}
