package org.okapi.rest.promql;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.google.gson.annotations.SerializedName;

/**
 * Matches SCALAR result – a 2-element array [timestamp, "value"].
 * Use a JsonDeserializer to bind to this class.
 */
@Data
@AllArgsConstructor
public class ScalarResult {
    @SerializedName(value = "0")
    private double timestamp;

    @SerializedName(value = "1")
    private String value;
}

