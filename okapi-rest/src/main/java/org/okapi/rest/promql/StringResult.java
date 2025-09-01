package org.okapi.rest.promql;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.google.gson.annotations.SerializedName;

/**
 * Matches STRING result – same structure as ScalarResult.
 */
@Data
@AllArgsConstructor
public class StringResult {
    @SerializedName(value = "0")
    private double timestamp;

    @SerializedName(value = "1")
    private String value;
}

