package org.okapi.rest.promql;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** One series in a MATRIX result. */
@Data
public class MatrixSeries {
    @SerializedName("metric")
    private Map<String, String> metric;

    @SerializedName("values")
    private List<Sample> values;
}
