package org.okapi.rest.promql;

import lombok.Data;
import com.google.gson.annotations.SerializedName;
import java.util.Map;

/** One series in a VECTOR result. */
@Data
public class VectorSeries {
    @SerializedName("metric")
    private Map<String, String> metric;

    @SerializedName("value")
    private Sample value;
}

