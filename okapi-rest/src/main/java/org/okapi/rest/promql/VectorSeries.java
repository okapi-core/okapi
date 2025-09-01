package org.okapi.rest.promql;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.google.gson.annotations.SerializedName;
import lombok.NoArgsConstructor;

import java.util.Map;

/** One series in a VECTOR result. */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class VectorSeries {
    @SerializedName("metric")
    private Map<String, String> metric;

    @SerializedName("value")
    private Sample value;
}

